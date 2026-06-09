"use client";

/**
 * i18next configuration for UI chrome (not curriculum content). Resources are
 * inlined to keep the prototype self-contained. Language codes map to the
 * three content languages: en / hi / mr.
 */
import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import type { Language } from "@/app/lib/types";

export const LANGUAGE_TO_CODE: Record<Language, string> = {
  English: "en",
  Hindi: "hi",
  Marathi: "mr",
};

const resources = {
  en: {
    translation: {
      appName: "EdgeEdu",
      tagline: "Offline-first learning, one question at a time",
      nav: { home: "Home", search: "Search", browse: "Browse", chat: "Chat" },
      search: {
        placeholder: "Ask a question or search a topic…",
        searching: "Building search index…",
        resultsCount: "{{count}} result",
        resultsCount_other: "{{count}} results",
        noResults: "No matches found. Try different keywords.",
        recent: "Recent searches",
        all: "All languages",
        sortBy: "Sort by",
        relevance: "Relevance",
        chapter: "Chapter",
      },
      browse: { title: "Browse curriculum", chunks: "topics" },
      content: { prerequisites: "Keywords", back: "Back to results", chapter: "Chapter" },
      home: {
        continue: "Continue learning",
        explore: "Explore subjects",
        empty: "Your viewed topics will appear here.",
      },
      chat: {
        title: "Ask EdgeEdu",
        header: "EdgeEdu AI Tutor",
        subtitle: "Ask any question about the Maharashtra State Board curriculum in English, Hindi, or Marathi. I'll find the most relevant content and answer with citations.",
        placeholder: "Type your question here…",
        trySaying: "Try asking",
        online: "Online",
        offline: "Offline — start the chatbot server",
        disclaimer: "Answers are generated from curriculum content and may not be perfect.",
        newChat: "New chat",
      },
    },
  },
  hi: {
    translation: {
      appName: "EdgeEdu",
      tagline: "ऑफ़लाइन-प्रथम शिक्षा, एक प्रश्न से शुरू",
      nav: { home: "होम", search: "खोज", browse: "ब्राउज़ करें", chat: "चैट" },
      search: {
        placeholder: "प्रश्न पूछें या विषय खोजें…",
        searching: "खोज सूची बन रही है…",
        resultsCount: "{{count}} परिणाम",
        resultsCount_other: "{{count}} परिणाम",
        noResults: "कोई परिणाम नहीं मिला। अन्य शब्द आज़माएँ।",
        recent: "हाल की खोजें",
        all: "सभी भाषाएँ",
        sortBy: "क्रमबद्ध करें",
        relevance: "प्रासंगिकता",
        chapter: "अध्याय",
      },
      browse: { title: "पाठ्यक्रम ब्राउज़ करें", chunks: "विषय" },
      content: { prerequisites: "मुख्य शब्द", back: "परिणामों पर वापस", chapter: "अध्याय" },
      home: {
        continue: "सीखना जारी रखें",
        explore: "विषय देखें",
        empty: "आपके देखे गए विषय यहाँ दिखेंगे।",
      },
      chat: {
        title: "EdgeEdu से पूछें",
        header: "EdgeEdu AI ट्यूटर",
        subtitle: "महाराष्ट्र राज्य बोर्ड पाठ्यक्रम के बारे में हिंदी, अंग्रेज़ी या मराठी में कोई भी प्रश्न पूछें।",
        placeholder: "अपना प्रश्न यहाँ लिखें…",
        trySaying: "ये पूछकर देखें",
        online: "ऑनलाइन",
        offline: "ऑफ़लाइन — चैटबॉट सर्वर शुरू करें",
        disclaimer: "उत्तर पाठ्यक्रम सामग्री से उत्पन्न हैं और पूर्ण रूप से सटीक नहीं हो सकते।",
        newChat: "नई चैट",
      },
    },
  },
  mr: {
    translation: {
      appName: "EdgeEdu",
      tagline: "ऑफलाइन-प्रथम शिक्षण, एका प्रश्नापासून",
      nav: { home: "मुख्यपृष्ठ", search: "शोध", browse: "ब्राउझ करा", chat: "चॅट" },
      search: {
        placeholder: "प्रश्न विचारा किंवा विषय शोधा…",
        searching: "शोध सूची तयार होत आहे…",
        resultsCount: "{{count}} निकाल",
        resultsCount_other: "{{count}} निकाल",
        noResults: "काही सापडले नाही. वेगळे शब्द वापरून पहा.",
        recent: "अलीकडील शोध",
        all: "सर्व भाषा",
        sortBy: "क्रमवारी",
        relevance: "सुसंगतता",
        chapter: "प्रकरण",
      },
      browse: { title: "अभ्यासक्रम ब्राउझ करा", chunks: "विषय" },
      content: { prerequisites: "मुख्य शब्द", back: "निकालांकडे परत", chapter: "प्रकरण" },
      home: {
        continue: "शिकणे सुरू ठेवा",
        explore: "विषय पाहा",
        empty: "तुम्ही पाहिलेले विषय येथे दिसतील.",
      },
      chat: {
        title: "EdgeEdu ला विचारा",
        header: "EdgeEdu AI ट्यूटर",
        subtitle: "महाराष्ट्र राज्य मंडळ अभ्यासक्रमाबद्दल मराठी, हिंदी किंवा इंग्रजीमध्ये कोणताही प्रश्न विचारा.",
        placeholder: "तुमचा प्रश्न येथे लिहा…",
        trySaying: "हे विचारून पहा",
        online: "ऑनलाइन",
        offline: "ऑफलाइन — चॅटबॉट सर्व्हर सुरू करा",
        disclaimer: "उत्तरे अभ्यासक्रमाच्या मजकुरातून तयार केली आहेत आणि पूर्णपणे अचूक नसू शकतात.",
        newChat: "नवीन चॅट",
      },
    },
  },
};

if (!i18n.isInitialized) {
  void i18n.use(initReactI18next).init({
    resources,
    lng: "en",
    fallbackLng: "en",
    interpolation: { escapeValue: false },
  });
}

export default i18n;
