# News Analyzer AI

<!-- ![Project Demo GIF](URL_TO_YOUR_DEMO_GIF_HERE) -->

<!-- > **Live Demo:** [https://your-live-demo-url.com](https://your-live-demo-url.com) -->

> **Backend Repository:** [news-analyzer-api](https://github.com/meeran-official/news-analyzer-api)

> **Frontend Repository:** [news-analyzer-ui](https://github.com/meeran-official/news-analyzer-ui)

## Project Summary

News Analyzer AI is a full-stack application designed to move beyond simple news aggregation. It leverages a multi-provider LLM backend to analyze trends from the past month, identifying systemic problems and proposing nuanced solutions. This project showcases advanced concepts like chained LLM calls, automated fallback mechanisms, caching strategies, and a dynamic, interactive frontend built with modern tools. The goal here is to build a product that provides insightful value.

## Key Features

- **Dynamic Topic Analysis:** Users can input any topic or select from AI-generated suggestions to receive a real-time analysis.
- **Aggregated Insights:** The AI synthesizes news from the past 30 days to identify deep, underlying problems, not just surface-level events.
- **Multi-Faceted Reports:** Each analysis includes a proposed solution, a look at opposing viewpoints, historical context, and more.
- **Resilient Backend:** Automatically falls back to a secondary LLM provider (OpenRouter) if the primary (Gemini) is rate-limited, ensuring high availability.
- **Optimized Performance:** Implements backend caching to reduce API calls and provides a responsive UI with loading states and animations.

## Technology Stack

### Backend (`news-analyzer-api`)

- **Framework:** Spring Boot 3
- **Language:** Java 17
- **Core Dependencies:** Spring Web, Spring Cache
- **Primary AI Provider:** Google Gemini API
- **Secondary AI Provider:** OpenRouter (routing to Kimi-V2, etc.)

### Frontend (`news-analyzer-ui`)

- **Framework:** Next.js 14+ (App Router)
- **Language:** TypeScript
- **Styling:** Tailwind CSS
- **Animation:** Framer Motion

## Setup and Installation

### Backend

1. Clone the repository: `git clone <repo-url>`
2. Navigate to the directory: `cd news-analyzer-api`
3. Create an `application.properties` file in `src/main/resources/` and add your API keys:
   ```properties
   news.api.key=<<YOUR_API_KEY>>

   # --- Primary LLM Provider (Gemini) ---
   llm.primary.provider=Gemini
   llm.primary.api.key=<<YOUR_API_KEY>>
   llm.primary.api.url=https://generativelanguage.googleapis.com/v1beta/models/
   llm.primary.model.analysis=gemini-2.5-flash-lite
   llm.primary.model.suggestions=gemini-2.0-flash-lite
   llm.primary.model.random=gemini-2.0-flash-lite

   # --- Secondary LLM Provider (OpenRouter) ---
   llm.secondary.provider=OpenRouter
   llm.secondary.api.key=<<YOUR_API_KEY>>
   llm.secondary.api.url=https://openrouter.ai/api/v1/chat/completions
   llm.secondary.model.analysis=moonshotai/kimi-k2:free 
   llm.secondary.model.suggestions=moonshotai/kimi-k2:free
   llm.secondary.model.random=moonshotai/kimi-k2:free
   
   # ... other properties (if needed)

4. Run the application: ./mvnw spring-boot:run

### Frontend
1. Clone the repository: git clone <repo-url>
2. Navigate to the directory: cd news-analyzer-ui
3. Install dependencies: npm install
4. Run the development server: npm run dev
