package com.luv2code.jobportal.services;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Uses a local LLM (llama3 via Ollama) to distil a raw resume into a
 * compact, signal-rich candidate profile string.
 *
 * Only skills, job titles, technologies, and years of experience are kept.
 * Noise like addresses, hobbies, personal statements, and formatting
 * artifacts are discarded so the downstream embedding is focused.
 */
@Service
public class ResumeSummaryService {

    private static final String SYSTEM_PROMPT =
            "You are an expert technical recruiter assistant. " +
            "Given the raw text of a candidate's resume, extract ONLY the following information " +
            "and output it as a single, dense paragraph with no bullet points, no headings, and no extra commentary:\n" +
            "- Technical skills and programming languages\n" +
            "- Frameworks, tools, and databases used\n" +
            "- Job titles held and core responsibilities\n" +
            "- Number of years of experience if mentioned\n\n" +
            "Do NOT include: names, addresses, phone numbers, emails, dates, GPA, hobbies, " +
            "awards, links, or any non-technical personal details.\n" +
            "Output ONLY the distilled paragraph. No preamble, no explanation.";

    private final ChatModel chatModel;

    @Autowired
    public ResumeSummaryService(@Qualifier("ollamaChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Summarises the raw resume text into a focused candidate profile.
     *
     * @param rawResumeText full text extracted from the PDF
     * @return compact profile string ready for embedding
     */
    public String summarise(String rawResumeText) {
        if (rawResumeText == null || rawResumeText.isBlank()) {
            return "";
        }

        // Truncate to first 4000 chars — llama3 context is large but we only
        // need the first portion which contains skills + experience
        String truncated = rawResumeText.length() > 4000
                ? rawResumeText.substring(0, 4000)
                : rawResumeText;

        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage("Resume:\n" + truncated)
        ));

        String summary = chatModel.call(prompt)
                .getResult()
                .getOutput()
                .getText();

        System.out.println("[ResumeSummaryService] Generated summary:\n" + summary);
        return summary;
    }
}
