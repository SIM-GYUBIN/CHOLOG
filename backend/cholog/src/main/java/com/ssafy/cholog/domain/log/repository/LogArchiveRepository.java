package com.ssafy.cholog.domain.log.repository;

import com.ssafy.cholog.domain.log.entity.LogArchive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogArchiveRepository extends JpaRepository<LogArchive, Integer> {

    Page<LogArchive> findAllArchiveLogByProjectId(Integer projectId, Pageable pageable);
}
