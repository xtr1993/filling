package com.filling.service;

import com.alibaba.fastjson.JSONObject;
import com.filling.client.ClusterClient;
import com.filling.domain.FillingJobs;
import com.filling.repository.FillingJobsRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service Implementation for managing {@link FillingJobs}.
 */
@Service
@Transactional
public class FillingJobsService {

    private final Logger log = LoggerFactory.getLogger(FillingJobsService.class);

    private final FillingJobsRepository fillingJobsRepository;

    private final ClusterClient clusterClient;

    public FillingJobsService(FillingJobsRepository fillingJobsRepository, ClusterClient clusterClient) {
        this.fillingJobsRepository = fillingJobsRepository;
        this.clusterClient = clusterClient;
    }

    /**
     * Save a fillingJobs.
     *
     * @param fillingJobs the entity to save.
     * @return the persisted entity.
     */
    public FillingJobs save(FillingJobs fillingJobs) {
        log.debug("Request to save FillingJobs : {}", fillingJobs);
        return fillingJobsRepository.save(fillingJobs);
    }

    /**
     * Partially update a fillingJobs.
     *
     * @param fillingJobs the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FillingJobs> partialUpdate(FillingJobs fillingJobs) {
        log.debug("Request to partially update FillingJobs : {}", fillingJobs);

        return fillingJobsRepository
            .findById(fillingJobs.getId())
            .map(
                existingFillingJobs -> {
                    if (fillingJobs.getName() != null) {
                        existingFillingJobs.setName(fillingJobs.getName());
                    }
                    if (fillingJobs.getApplicationId() != null) {
                        existingFillingJobs.setApplicationId(fillingJobs.getApplicationId());
                    }
                    if (fillingJobs.getJobText() != null) {
                        existingFillingJobs.setJobText(fillingJobs.getJobText());
                    }
                    if (fillingJobs.getType() != null) {
                        existingFillingJobs.setType(fillingJobs.getType());
                    }
                    if (fillingJobs.getConfProp() != null) {
                        existingFillingJobs.setConfProp(fillingJobs.getConfProp());
                    }
                    if (fillingJobs.getStatus() != null) {
                        existingFillingJobs.setStatus(fillingJobs.getStatus());
                    }
                    if (fillingJobs.getCreatetime() != null) {
                        existingFillingJobs.setCreatetime(fillingJobs.getCreatetime());
                    }
                    if (fillingJobs.getCreatedBy() != null) {
                        existingFillingJobs.setCreatedBy(fillingJobs.getCreatedBy());
                    }
                    if (fillingJobs.getAddjar() != null) {
                        existingFillingJobs.setAddjar(fillingJobs.getAddjar());
                    }
                    if (fillingJobs.getDescription() != null) {
                        existingFillingJobs.setDescription(fillingJobs.getDescription());
                    }

                    existingFillingJobs.setUpdatetime(Instant.now());

                    return existingFillingJobs;
                }
            )
            .map(fillingJobsRepository::save);
    }

    /**
     * Get all the fillingJobs.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<FillingJobs> findAll(Pageable pageable) {
        log.debug("Request to get all FillingJobs");
        return fillingJobsRepository.findAll(pageable);
    }

    /**
     * Get one fillingJobs by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<FillingJobs> findOne(Long id) {
        log.debug("Request to get FillingJobs : {}", id);
        return fillingJobsRepository.findById(id);
    }

    /**
     * Delete the fillingJobs by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete FillingJobs : {}", id);
        fillingJobsRepository.deleteById(id);
    }

    /**
     * start thes filling job
     *
     * @param fillingJobs
     * @return
     */
    public FillingJobs start(FillingJobs fillingJobs) {
        Optional<String> jobId = clusterClient.submit(fillingJobs.toJobString());
        if (jobId.isPresent()) {
            fillingJobs.setStatus("2");
            fillingJobs.setApplicationId(jobId.get());
            save(fillingJobs);
        } else {
            fillingJobs.setStatus("6");
            save(fillingJobs);
        }
        return fillingJobs;
    }

    /**
     * stop this job
     *
     * @param fillingJobs
     * @return
     */
    public FillingJobs stop(FillingJobs fillingJobs) {
        if (StringUtils.isEmpty(fillingJobs.getApplicationId())) {
            log.warn("jobid is null {}", fillingJobs);
            // TODO jobid is null 的逻辑
            return fillingJobs;
        }

        Boolean result = clusterClient.cancel(fillingJobs.getApplicationId());
        if (result) {
            fillingJobs.setStatus("5");
        } else {
            // TODO 停止失败的逻辑
        }
        return save(fillingJobs);
    }

    /**
     * plan this job
     *
     * @param fillingJobs
     * @return
     */
    public JSONObject plan(FillingJobs fillingJobs) {
        return clusterClient.plan(fillingJobs.toJobString());
    }
}
