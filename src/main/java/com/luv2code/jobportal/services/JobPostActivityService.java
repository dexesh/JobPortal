package com.luv2code.jobportal.services;

import com.luv2code.jobportal.entity.*;
import com.luv2code.jobportal.repository.JobPostActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

@Service
public class JobPostActivityService {
    private static final Logger log = LoggerFactory.getLogger(JobPostActivityService.class);
   @Autowired
    private final JobPostActivityRepository jobPostActivityRepository;
   @Autowired
   private final CreateEmbeddingService embeddingService;
   private final JobIndexVersionService jobIndexVersionService;
   private final AtomicBoolean indexBackfilled = new AtomicBoolean(false);

    public JobPostActivityService(JobPostActivityRepository jobPostActivityRepository,
                                  CreateEmbeddingService embeddingService,
                                  JobIndexVersionService jobIndexVersionService) {
        this.jobPostActivityRepository = jobPostActivityRepository;
        this.embeddingService = embeddingService;
        this.jobIndexVersionService = jobIndexVersionService;
    }

    public JobPostActivity addNew(JobPostActivity jobPostActivity) {
        JobPostActivity savedjob= jobPostActivityRepository.save(jobPostActivity);
        embeddingService.createandStoreEmbedding(savedjob);
        jobIndexVersionService.increment();
        return savedjob;
    }

    public List<RecruiterJobsDto> getRecruiterJobs(int recruiter) {

        List<IRecruiterJobs> recruiterJobsDtos = jobPostActivityRepository.getRecruiterJobs(recruiter);

        List<RecruiterJobsDto> recruiterJobsDtoList = new ArrayList<>();

        for (IRecruiterJobs rec : recruiterJobsDtos) {
            JobLocation loc = new JobLocation(rec.getLocationId(), rec.getCity(), rec.getState(), rec.getCountry());
            JobCompany comp = new JobCompany(rec.getCompanyId(), rec.getName(), "");
            recruiterJobsDtoList.add(new RecruiterJobsDto(rec.getTotalCandidates(), rec.getJob_post_id(),
                    rec.getJob_title(), loc, comp));
        }
        return recruiterJobsDtoList;

    }

    public JobPostActivity getOne(int id) {
        return jobPostActivityRepository.findById(id).orElseThrow(()->new RuntimeException("Job not found"));
    }

    public List<JobPostActivity> getAll() {
        return jobPostActivityRepository.findAll();
    }

    /** Rebuilds Pinecone after startup, outside any browser request. */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void backfillVectorIndex() {
        if (indexBackfilled.get()) return;
        String lockOwner = UUID.randomUUID().toString();
        if (!jobIndexVersionService.tryAcquireBackfillLock(lockOwner)) {
            indexBackfilled.set(true);
            log.info("[Pinecone] Another application instance is performing the vector backfill.");
            return;
        }
        int indexed = 0;
        try {
            for (JobPostActivity job : jobPostActivityRepository.findAll()) {
                try {
                    embeddingService.createandStoreEmbedding(job);
                    indexed++;
                } catch (Exception exception) {
                    log.error("[Pinecone] Could not backfill jobId={}", job.getJobPostId(), exception);
                }
            }
            indexBackfilled.set(true);
            if (indexed > 0) jobIndexVersionService.increment();
            log.info("[Pinecone] Background backfill finished; indexedJobs={}", indexed);
        } finally {
            jobIndexVersionService.releaseBackfillLock(lockOwner);
        }
    }

    public List<JobPostActivity> search(String job, String location, List<String> type, List<String> remote, LocalDate searchDate) {
        return Objects.isNull(searchDate) ? jobPostActivityRepository.searchWithoutDate(job, location, remote,type) :
                jobPostActivityRepository.search(job, location, remote, type, searchDate);
    }

}
