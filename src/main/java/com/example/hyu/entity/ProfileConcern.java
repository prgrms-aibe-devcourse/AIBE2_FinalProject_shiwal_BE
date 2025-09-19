package com.example.hyu.entity;

import com.example.hyu.enums.ProfileConcernTag;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "profile_concern_tags")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@IdClass(ProfileConcern.PK.class)
public class ProfileConcern {

    @Id
    private Long userId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ProfileConcernTag tag;

    @Data
    public static class PK implements Serializable {
        private Long userId;
        private ProfileConcernTag tag;
    }
}
