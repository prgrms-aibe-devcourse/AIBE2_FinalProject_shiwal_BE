package com.example.hyu.service.community;

import com.example.hyu.dto.community.PollCreateRequest;
import com.example.hyu.dto.community.PollDetailResponse;
import com.example.hyu.dto.community.PollResultResponse;
import com.example.hyu.dto.community.PollVoteRequest;
import com.example.hyu.entity.*;
import com.example.hyu.repository.UserRepository;
import com.example.hyu.repository.community.CommunityPostRepository;
import com.example.hyu.repository.community.PollOptionRepository;
import com.example.hyu.repository.community.PollRepository;
import com.example.hyu.repository.community.PollVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PollService {
    private final PollRepository pollRepo;
    private final PollOptionRepository optionRepo;
    private final PollVoteRepository voteRepo;
    private final CommunityPostRepository postRepo;
    private final UserRepository userRepo;

    // 생성
    // 게시글 마다 1개만 허용
    // deadlind이 null이면 +24h
    // BINARY면 옵션 비어있을 시 예, 아니오 자동 생성
    @Transactional
    public Long create(Long postId, PollCreateRequest req, Long requesterUserId, boolean isAdmin) {
        // 게시글 존재 확인
        CommunityPost post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("POST_NOT_FOUND"));

        // 게시글 작성자 or 관리자만 허용
        Long authorId = post.getAuthor() != null ? post.getAuthor().getId() : null;
        if(!Objects.equals(authorId, requesterUserId) && !isAdmin) {
            throw new RuntimeException("FORBIDDEN_NOT_OWNER");
        }

        //게시글 당 투표 1개 제한
        if (pollRepo.findByPostId(postId).isPresent()) {
            throw new RuntimeException("POLL_ALREADY_EXISTS_FOR_POST");
        }

        //마감일 기본값 처리
        Instant deadline = Poll.resolveDeadline(req.deadline());

        // 엔티티 생성
        Poll.PollType type = Poll.PollType.valueOf(req.type().name());
        Poll poll = Poll.builder()
                .post(post)
                .type(type)
                .question(req.question())
                .deadline(deadline)
                .deleted(false)
                .build();

        // 옵션 생성
        if(type == Poll.PollType.BINARY) {
            // 요청에서 옵션이 안 왔거나 비었으면 기본 [예, 아니오]
            List<String> labels =  (req.options() == null || req.options().isEmpty())
                    ? List.of("예", "아니오")
                    : req.options();
            ensureBinaryTwoOptions(labels);
            labels.forEach(label -> poll.addOption(PollOption.builder().content(label).build()));
        } else { // SINGLE
            if(req.options() == null || req.options().isEmpty()) {
                throw new RuntimeException("OPTIONS_REQUIRED_FOR_SINGLE");
            }
            req.options().forEach(label -> poll.addOption(PollOption.builder().content(label).build()));
        }

        return pollRepo.save(poll).getId();
    }

    //BINARY 일떄 옵션 2개 강제 (예 아니오)
    private void ensureBinaryTwoOptions(List<String> labels) {
        if(labels.size() != 2) {
            throw new RuntimeException("BINARY_REQUIRES_EXACTLY_TWO_OPTIONS");
        }
    }

    // 단건 조회 (상세)
    public PollDetailResponse getDetail(Long pollId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new RuntimeException("POLL_NOT_FOUNT"));

        var options = optionRepo.findByPollId(pollId).stream()
                .map(o -> new PollDetailResponse.OptionItem(o.getId(), o.getContent()))
                .toList();

        return new PollDetailResponse(
                poll.getId(),
                poll.getType().name(),
                poll.getQuestion(),
                poll.getDeadline(),
                options
        );
    }

    // 게시글에 달리 ㄴ투표 조회
    public Optional<PollDetailResponse> getByPostId(Long postId) {
        return pollRepo.findByPostId(postId).map(poll -> {
            var options = optionRepo.findByPollId(poll.getId()).stream()
                    .map(o -> new PollDetailResponse.OptionItem(o.getId(), o.getContent()))
                    .toList();

            return new PollDetailResponse(
                    poll.getId(),
                    poll.getType().name(),
                    poll.getQuestion(),
                    poll.getDeadline(),
                    options
            );
        });
    }

    // 투표하기 (단일선택)
    // 마감 이후 불가, 선택한 option이 해당 poll 소속인지 검증
    // user, poll 중복 방지 -> 이미 있으면 에러 처리
    @Transactional
    public void vote(Long userId, Long pollId, PollVoteRequest req) {
        Users user = userRepo.findById(userId)
                .orElseThrow(()->new RuntimeException("USER_NOT_FOUND"));

        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new RuntimeException("POLL_NOT_FOUND"));

        if (poll.isExpired()) {
            throw new RuntimeException("POLL_CLOSED");
        }

        // 중복 투표 방지
        if (voteRepo.findByUserIdAndPollId(userId, pollId).isPresent()) {
            throw new RuntimeException("ALREADY_VOTED");
        }

        // option 유효성 : 해당 poll의 옵션인지 확인
        PollOption option = optionRepo.findById(req.optionId())
                .orElseThrow(() -> new RuntimeException("OPTION_NOT_FOUND"));
        if(!option.getPoll().getId().equals(pollId)) {
            throw new RuntimeException("OPTION_DOES_NOT_BELONG_TO_POLL");
        }

        //저장
        voteRepo.save(PollVote.builder()
                .user(user)
                .poll(poll)
                .option(option)
                .build());
    }

    // 결과 조회(집계 + 비율)
    public PollResultResponse getResults(Long pollId) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new RuntimeException("POLL_NOT_FOUND"));

        // 옵션 목록 캐시 (optionId -> content) : 결과 반환 시 이름 lookup
        Map<Long, String> optionTextById = optionRepo.findByPollId(pollId).stream()
                .collect(Collectors.toMap(PollOption::getId, PollOption::getContent));

        // 옵션 별 집계 [optionId, count]
        List<Object[]> rows = voteRepo.countVotesByPollGrouped(pollId);
        long total = rows.stream().mapToLong(r -> (Long) r[1]).sum();

        List<PollResultResponse.OptionResult> optionResults = new ArrayList<>();
        for(Object[] r: rows) {
            Long optionId = (Long) r[0];
            long count = (Long) r[1];
            String content = optionTextById.getOrDefault(optionId, "(삭제됨)");
            double percentage = (total == 0 ) ? 0.0 : (count * 100.0 / total);
            optionResults.add(new PollResultResponse.OptionResult(optionId, content, count, percentage));
        }

        return new PollResultResponse(
                poll.getId(),
                poll.getQuestion(),
                poll.getDeadline(),
                total,
                optionResults
        );
    }

    /* =========================
        투표 삭제
       - soft delete: @SQLDelete 로 마킹
       ========================= */
    @Transactional
    public void delete(Long pollId, Long requesterId, boolean isAdmin) {
        Poll poll = pollRepo.findById(pollId)
                .orElseThrow(() -> new RuntimeException("POLL_NOT_FOUND"));

        // 작성자만 or 관리자만 삭제: 게시글 작성자 비교
        Long authorId = poll.getPost().getAuthor() != null ? poll.getPost().getAuthor().getId() : null;
        boolean owner = Objects.equals(authorId, requesterId);
        if (!owner && !isAdmin) throw new RuntimeException("FORBIDDEN_NOT_OWNER");

        pollRepo.delete(poll); // @SQLDelete 로 soft delete
    }
}


