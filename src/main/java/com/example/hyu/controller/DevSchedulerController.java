package com.example.hyu.controller;


import com.example.hyu.scheduler.GoalNotificationScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dev")
public class DevSchedulerController {
    private final GoalNotificationScheduler scheduler;

    /** 한 번만 수동 실행 */
    @PostMapping("/run-notification-job")
    public String runJobOnce() {
        scheduler.runDaily();
        return "OK";
    }
}
