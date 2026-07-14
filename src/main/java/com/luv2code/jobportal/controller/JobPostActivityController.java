package com.luv2code.jobportal.controller;

import com.luv2code.jobportal.entity.*;
import com.luv2code.jobportal.services.JobPostActivityService;
import com.luv2code.jobportal.services.JobRecommendationService;
import com.luv2code.jobportal.services.JobSeekerApplyService;
import com.luv2code.jobportal.services.JobSeekerSaveService;
import com.luv2code.jobportal.services.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Controller
public class JobPostActivityController {

    private final UsersService usersService;
    private final JobPostActivityService jobPostActivityService;
    private final JobSeekerApplyService jobSeekerApplyService;
    private final JobSeekerSaveService jobSeekerSaveService;
    private final JobRecommendationService jobRecommendationService;

    @Autowired
    public JobPostActivityController(UsersService usersService, JobPostActivityService jobPostActivityService,
            JobSeekerApplyService jobSeekerApplyService, JobSeekerSaveService jobSeekerSaveService,
            JobRecommendationService jobRecommendationService) {
        this.usersService = usersService;
        this.jobPostActivityService = jobPostActivityService;
        this.jobSeekerApplyService = jobSeekerApplyService;
        this.jobSeekerSaveService = jobSeekerSaveService;
        this.jobRecommendationService = jobRecommendationService;
    }

    @GetMapping("/dashboard/")
    public String searchJobs(Model model,
            @RequestParam(value = "job", required = false) String job,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "partTime", required = false) String partTime,
            @RequestParam(value = "fullTime", required = false) String fullTime,
            @RequestParam(value = "freelance", required = false) String freelance,
            @RequestParam(value = "remoteOnly", required = false) String remoteOnly,
            @RequestParam(value = "officeOnly", required = false) String officeOnly,
            @RequestParam(value = "partialRemote", required = false) String partialRemote,
            @RequestParam(value = "today", required = false) boolean today,
            @RequestParam(value = "days7", required = false) boolean days7,
            @RequestParam(value = "days30", required = false) boolean days30

    ) {

        model.addAttribute("partTime", Objects.equals(partTime, "Part-Time"));
        model.addAttribute("fullTime", Objects.equals(fullTime, "Full-Time"));
        model.addAttribute("freelance", Objects.equals(freelance, "Freelance"));

        model.addAttribute("remoteOnly", Objects.equals(remoteOnly, "Remote-Only"));
        model.addAttribute("officeOnly", Objects.equals(officeOnly, "Office-Only"));
        model.addAttribute("partialRemote", Objects.equals(partialRemote, "Partial-Remote"));

        model.addAttribute("today", today);
        model.addAttribute("days7", days7);
        model.addAttribute("days30", days30);

        model.addAttribute("job", job);
        model.addAttribute("location", location);

        LocalDate searchDate = null;
        List<JobPostActivity> jobPost = null;
        boolean dateSearchFlag = true;
        boolean remote = true;
        boolean type = true;

        if (days30) {
            searchDate = LocalDate.now().minusDays(30);
        } else if (days7) {
            searchDate = LocalDate.now().minusDays(7);
        } else if (today) {
            searchDate = LocalDate.now();
        } else {
            dateSearchFlag = false;
        }

        if (partTime == null && fullTime == null && freelance == null) {
            partTime = "Part-Time";
            fullTime = "Full-Time";
            freelance = "Freelance";
            remote = false;
        }

        if (officeOnly == null && remoteOnly == null && partialRemote == null) {
            officeOnly = "Office-Only";
            remoteOnly = "Remote-Only";
            partialRemote = "Partial-Remote";
            type = false;
        }

        if (!dateSearchFlag && !remote && !type && !StringUtils.hasText(job) && !StringUtils.hasText(location)) {
            jobPost = jobPostActivityService.getAll();
        } else {
            jobPost = jobPostActivityService.search(job, location, Arrays.asList(partTime, fullTime, freelance),
                    Arrays.asList(remoteOnly, officeOnly, partialRemote), searchDate);
        }

        Object currentUserProfile = usersService.getCurrentUserProfile();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUsername = authentication.getName();
            model.addAttribute("username", currentUsername);
            if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("Recruiter"))) {
                List<RecruiterJobsDto> recruiterJobs = jobPostActivityService
                        .getRecruiterJobs(((RecruiterProfile) currentUserProfile).getUserAccountId());
                model.addAttribute("jobPost", recruiterJobs);
            } else {
                List<JobSeekerApply> jobSeekerApplyList = jobSeekerApplyService
                        .getCandidatesJobs((JobSeekerProfile) currentUserProfile);
                List<JobSeekerSave> jobSeekerSaveList = jobSeekerSaveService
                        .getCandidatesJob((JobSeekerProfile) currentUserProfile);

                boolean exist;
                boolean saved;

                for (JobPostActivity jobActivity : jobPost) {
                    exist = false;
                    saved = false;
                    for (JobSeekerApply jobSeekerApply : jobSeekerApplyList) {
                        if (Objects.equals(jobActivity.getJobPostId(), jobSeekerApply.getJob().getJobPostId())) {
                            jobActivity.setIsActive(true);
                            exist = true;
                            break;
                        }
                    }

                    for (JobSeekerSave jobSeekerSave : jobSeekerSaveList) {
                        if (Objects.equals(jobActivity.getJobPostId(), jobSeekerSave.getJob().getJobPostId())) {
                            jobActivity.setIsSaved(true);
                            saved = true;
                            break;
                        }
                    }

                    if (!exist) {
                        jobActivity.setIsActive(false);
                    }
                    if (!saved) {
                        jobActivity.setIsSaved(false);
                    }

                    model.addAttribute("jobPost", jobPost);

                }
            }
        }

        model.addAttribute("user", currentUserProfile);

        return "dashboard";
    }

    @GetMapping("/dashboard/recommendations")
    @ResponseBody
    public CompletableFuture<java.util.Map<String, Object>> getRecommendations() {

        Object currentUserProfile = usersService.getCurrentUserProfile();

        if (!(currentUserProfile instanceof JobSeekerProfile)) {
            return CompletableFuture.completedFuture(recommendationPayload(
                    "FORBIDDEN", "Recommendations are available to job seekers only.", java.util.Collections.emptyList()));
        }

        JobSeekerProfile seekerProfile = (JobSeekerProfile) currentUserProfile;

        String resumeFileName = seekerProfile.getResume();

        if (resumeFileName == null || resumeFileName.isEmpty()) {
            return CompletableFuture.completedFuture(recommendationPayload(
                    "NO_RESUME", "Upload a resume to get personalised recommendations.", java.util.Collections.emptyList()));
        }

        String resumePath = "photos/candidate/" +
                seekerProfile.getUserAccountId() +
                "/" +
                resumeFileName;

        return jobRecommendationService
                .getRecommendedJobs(seekerProfile.getUserAccountId(), resumePath)
                .thenApply(recommendation -> {

                    List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

                    for (JobPostActivity j : recommendation.jobs()) {

                        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();

                        map.put("jobPostId", j.getJobPostId());
                        map.put("jobTitle", j.getJobTitle());

                        result.add(map);
                    }

                    return recommendationPayload(recommendation.status(), recommendation.message(), result);
                });
    }

    private java.util.Map<String, Object> recommendationPayload(
            String status, String message, List<java.util.Map<String, Object>> jobs) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("status", status);
        payload.put("message", message);
        payload.put("jobs", jobs);
        return payload;
    }

    @GetMapping("global-search/")
    public String globalSearch(Model model,
            @RequestParam(value = "job", required = false) String job,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "partTime", required = false) String partTime,
            @RequestParam(value = "fullTime", required = false) String fullTime,
            @RequestParam(value = "freelance", required = false) String freelance,
            @RequestParam(value = "remoteOnly", required = false) String remoteOnly,
            @RequestParam(value = "officeOnly", required = false) String officeOnly,
            @RequestParam(value = "partialRemote", required = false) String partialRemote,
            @RequestParam(value = "today", required = false) boolean today,
            @RequestParam(value = "days7", required = false) boolean days7,
            @RequestParam(value = "days30", required = false) boolean days30) {

        model.addAttribute("partTime", Objects.equals(partTime, "Part-Time"));
        model.addAttribute("fullTime", Objects.equals(fullTime, "Full-Time"));
        model.addAttribute("freelance", Objects.equals(freelance, "Freelance"));

        model.addAttribute("remoteOnly", Objects.equals(remoteOnly, "Remote-Only"));
        model.addAttribute("officeOnly", Objects.equals(officeOnly, "Office-Only"));
        model.addAttribute("partialRemote", Objects.equals(partialRemote, "Partial-Remote"));

        model.addAttribute("today", today);
        model.addAttribute("days7", days7);
        model.addAttribute("days30", days30);

        model.addAttribute("job", job);
        model.addAttribute("location", location);

        LocalDate searchDate = null;
        List<JobPostActivity> jobPost = null;
        boolean dateSearchFlag = true;
        boolean remote = true;
        boolean type = true;

        if (days30) {
            searchDate = LocalDate.now().minusDays(30);
        } else if (days7) {
            searchDate = LocalDate.now().minusDays(7);
        } else if (today) {
            searchDate = LocalDate.now();
        } else {
            dateSearchFlag = false;
        }

        if (partTime == null && fullTime == null && freelance == null) {
            partTime = "Part-Time";
            fullTime = "Full-Time";
            freelance = "Freelance";
            remote = false;
        }

        if (officeOnly == null && remoteOnly == null && partialRemote == null) {
            officeOnly = "Office-Only";
            remoteOnly = "Remote-Only";
            partialRemote = "Partial-Remote";
            type = false;
        }

        if (!dateSearchFlag && !remote && !type && !StringUtils.hasText(job) && !StringUtils.hasText(location)) {
            jobPost = jobPostActivityService.getAll();
        } else {
            jobPost = jobPostActivityService.search(job, location, Arrays.asList(partTime, fullTime, freelance),
                    Arrays.asList(remoteOnly, officeOnly, partialRemote), searchDate);
        }

        model.addAttribute("jobPost", jobPost);
        return "global-search";
    }

    @GetMapping("/dashboard/add")
    public String addJobs(Model model) {
        model.addAttribute("jobPostActivity", new JobPostActivity());
        model.addAttribute("user", usersService.getCurrentUserProfile());
        return "add-jobs";
    }

    @PostMapping("/dashboard/addNew")
    public String addNew(JobPostActivity jobPostActivity, Model model) {

        Users user = usersService.getCurrentUser();
        if (jobPostActivity.getJobPostId() != null) {
            JobPostActivity existing = jobPostActivityService.getOne(jobPostActivity.getJobPostId());
            if (user == null || existing.getPostedById() == null
                    || !Objects.equals(existing.getPostedById().getUserId(), user.getUserId())) {
                throw new org.springframework.security.access.AccessDeniedException("You do not own this job posting");
            }
        }
        if (user != null) {
            jobPostActivity.setPostedById(user);
        }
        jobPostActivity.setPostedDate(new Date());
        model.addAttribute("jobPostActivity", jobPostActivity);
        JobPostActivity saved = jobPostActivityService.addNew(jobPostActivity);
        return "redirect:/dashboard/";
    }

    @GetMapping("dashboard/edit/{id}")
    public String editJob(@PathVariable("id") int id, Model model) {

        JobPostActivity jobPostActivity = jobPostActivityService.getOne(id);
        Users user = usersService.getCurrentUser();
        if (jobPostActivity.getPostedById() == null || user == null
                || !Objects.equals(jobPostActivity.getPostedById().getUserId(), user.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not own this job posting");
        }
        model.addAttribute("jobPostActivity", jobPostActivity);
        model.addAttribute("user", usersService.getCurrentUserProfile());
        return "add-jobs";
    }
}
