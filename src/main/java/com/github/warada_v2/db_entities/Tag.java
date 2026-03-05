package com.github.warada_v2.db_entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Table(name = "tags")
@Builder
@Entity
@ToString(exclude = "comics")
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tag_id_seq")
    @SequenceGenerator(name = "tag_id_seq", sequenceName = "tag_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false, unique = true)
    private String normalizedName;

    @Column(name = "comic_count", nullable = false)
    @Builder.Default
    private Integer comicCount = 0;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Comic> comics = new ArrayList<>();
}
