#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Construye el JSON final curado de los 20 articulos."""
import json, os, re

BASE = os.path.dirname(__file__)
borrador = json.load(open(os.path.join(BASE, "articulos_borrador2.json"), encoding="utf-8"))
by_id = {r["id"]: r for r in borrador}

# Titulo y autores verificados manualmente contra el encabezado de cada PDF.
META = {
    1: ("Artificial intelligence literacy in higher and adult education: A scoping literature review",
        "Matthias Carl Laupichler, Alexandra Aster, Jana Schirch, Tobias Raupach"),
    2: ("Systematic literature review on opportunities, challenges, and future research recommendations of artificial intelligence in education",
        "Thomas K.F. Chiu, Qi Xia, Xinyan Zhou, Ching Sing Chai, Miaoting Cheng"),
    3: ("Myths, mis- and preconceptions of artificial intelligence: A review of the literature",
        "Arne Bewersdorff, Xiaoming Zhai, Jessica Roberts, Claudia Nerdel"),
    4: ("Opportunities of artificial intelligence for supporting complex problem-solving: Findings from a scoping review",
        "Srecko Joksimovic, Dirk Ifenthaler, Rebecca Marrone, Maarten De Laat, George Siemens"),
    5: ("Application and theory gaps during the rise of Artificial Intelligence in Education",
        "Xieling Chen, Haoran Xie, Di Zou, Gwo-Jen Hwang"),
    6: ("A multi-perspective study on Artificial Intelligence in Education: grants, conferences, journals, software tools, institutions, and researchers",
        "Xieling Chen, Haoran Xie, Gwo-Jen Hwang"),
    7: ("Human-centred learning analytics and AI in education: A systematic literature review",
        "Riordan Alfredo, Vanessa Echeverria, Yueqiao Jin, Lixiang Yan, Zachari Swiecki, Dragan Gasevic, Roberto Martinez-Maldonado"),
    8: ("A systematic review of artificial intelligence techniques for collaborative learning over the past two decades",
        "Seng Chee Tan, Alwyn Vwen Yen Lee, Min Lee"),
    9: ("AI technologies for education: Recent research & future directions",
        "Ke Zhang, Ayse Begum Aslan"),
    10: ("Educational applications of artificial intelligence in simulation-based learning: A systematic mapping review",
         "Chih-Pu Dai, Fengfeng Ke"),
    11: ("A scoping review on how generative artificial intelligence transforms assessment in higher education",
         "Qi Xia, Xiaojing Weng, Fan Ouyang, Tzung Jin Lin, Thomas K.F. Chiu"),
    12: ("A meta systematic review of artificial intelligence in higher education: a call for increased ethics, collaboration, and rigour",
         "Melissa Bond, Hassan Khosravi, Maarten De Laat, Nina Bergdahl, Violeta Negrea, Emily Oxley, Phuong Pham, Sin Wang Chong, George Siemens"),
    13: ("Artificial intelligence for education and its emphasis on assessment and adversity quotient: a review",
         "Jyoti Mudkanna Gavhane, Reena Pagare"),
    14: ("A comprehensive exploration of artificial intelligence competence frameworks for educators: A critical review",
         "Tamar Mikeladze, Paulien C. Meijer, Roald P. Verhoeff"),
    15: ("A Systematic Review of Generative AI for Teaching and Learning Practice",
         "Bayode Ogunleye, Kudirat Ibilola Zakariyyah, Oluwaseun Ajao, Olakunle Olayinka, Hemlata Sharma"),
    16: ("Generative Artificial Intelligence in Tertiary Education: Assessment Redesign Principles and Considerations",
         "Che Yee Lye, Lyndon Lim"),
    17: ("A Systematic Review of AI-Driven Educational Assessment in STEM Education",
         "Fan Ouyang, Tuan Anh Dinh, Weiqi Xu"),
    18: ("Transforming Education: A Comprehensive Review of Generative Artificial Intelligence in Educational Settings through Bibliometric and Content Analysis",
         "Zied Bahroun, Chiraz Anane, Vian Ahmed, Amani Zacca"),
    19: ("What Is the Impact of ChatGPT on Education? A Rapid Review of the Literature",
         "Chung Kwan Lo"),
    20: ("Systematic review of research on artificial intelligence applications in higher education: where are the educators?",
         "Olaf Zawacki-Richter, Victoria I. Marin, Melissa Bond, Franziska Gouverneur"),
}

# Abstracts corregidos manualmente para los articulos donde la extraccion
# automatica no funciono bien (columnas / notas al pie intercaladas).
ABSTRACT_OVERRIDES = {
    11: ("Generative artificial intelligence provides both opportunities and challenges for higher "
         "education. Existing literature has not properly investigated how this technology would "
         "impact assessment in higher education. This scoping review took a forward-thinking "
         "approach to investigate how generative artificial intelligence transforms assessment "
         "in higher education. We used the PRISMA extension for scoping reviews to select articles "
         "for review and report the results. In the screening, we retrieved 969 articles and selected "
         "32 empirical studies for analysis. Most of the articles were published in 2023. We used "
         "three levels—students, teachers, and institutions—to analyses the articles. Our results "
         "suggested that assessment should be transformed to cultivate students' self-regulated "
         "learning skills, responsible learning, and integrity. To successfully transform assessment "
         "in higher education, the review suggested that (i) teacher professional development "
         "activities for assessment, AI, and digital literacy should be provided, (ii) teachers' beliefs "
         "about human and AI assessment should be strengthened, and (iii) teachers should be "
         "innovative and holistic in their teaching to reflect the assessment transformation. "
         "Educational institutions are recommended to review and rethink their assessment policies, "
         "as well as provide more inter-disciplinary programs and teaching."),
    14: ("Recent literature underscores the need for teachers to develop AI competencies with a "
         "recognition of the current lack of well-defined competence frameworks. This critical review "
         "investigates teachers' Artificial Intelligence (AI) competence frameworks (AI CFTs), analysing "
         "their strengths, weaknesses and practical applications for researchers, educators and "
         "policymakers. It identifies five distinct types of AI CFTs, each addressing the complexities of "
         "AI in its early stages. Notably, frameworks derived from empirical data offer detailed "
         "descriptions of competencies, while those based on conceptual models provide broader "
         "overviews. Highlighting the need for further empirical research, this review helps identify "
         "and understand existing approaches to teacher AI competence development and paves the way "
         "for integrating AI CFTs into teacher education, ultimately enhancing educators' preparedness "
         "to harness AI in their teaching practices."),
    17: ("Artificial intelligence (AI), as an emerging technology, has been widely used in "
         "STEM education to promote the educational assessment. Although AI-driven educational "
         "assessment has the potential to assess students' learning automatically and "
         "reduce the workload of instructors, there is still a lack of review works to holistically "
         "examine the field of AI-driven educational assessment, especially in the STEM "
         "education context. To gain an overview of the application of AI-driven educational "
         "assessment in STEM education, this research conducted a systematic review based "
         "on 17 empirical research published from 2011 January to 2023 April. Specifically, "
         "this review examined the functions, algorithms, and effects of AI applications in "
         "STEM educational assessment. The results clarified three main functions of AI-driven "
         "educational assessment, namely academic performance assessment, learning status "
         "assessment, and instructional quality assessment. Moreover, the systematic review found "
         "that both traditional algorithms (e.g., natural language processing, machine learning) "
         "and advanced algorithms (e.g., deep learning, neural fuzzy systems) were applied in STEM "
         "educational assessment. Furthermore, the educational and technological effects of applying "
         "AI-driven educational assessment in STEM education were revealed. Based on the results, "
         "this research proposed educational and technological implications to guide the future "
         "practice and research of AI-driven educational assessment in STEM education."),
    9: ("From unique educational perspectives, this article reports a comprehensive review of selected "
        "empirical studies on artificial intelligence in education (AIEd) published in 1993–2020, as "
        "collected in the Web of Sciences database and selected AIEd-specialized journals. A total of 40 "
        "empirical studies met all selection criteria, and were fully reviewed using multiple methods, "
        "including selected bibliometrics, content analysis and categorical meta-trends analysis. This "
        "article reports the current state of AIEd research, highlights selected AIEd technologies and "
        "applications, reviews their proven and potential benefits for education, bridges the gaps between "
        "AI technological innovations and their educational applications, and generates practical examples "
        "and inspirations for both technological experts that create AIEd technologies and educators who "
        "spearhead AI innovations in education. It also provides rich discussions on practical implications "
        "and future research directions from multiple perspectives. The advancement of AIEd calls for "
        "critical initiatives to address AI ethics and privacy concerns, and requires interdisciplinary and "
        "transdisciplinary collaborations in large-scaled, longitudinal research and development efforts."),
    12: ("Although the field of Artificial Intelligence in Education (AIEd) has a substantial history "
         "as a research domain, never before has the rapid evolution of AI applications in education sparked "
         "such prominent public discourse. Given the already rapidly growing AIEd literature base in higher "
         "education, now is the time to ensure that the field has a solid research and conceptual grounding. "
         "This review of reviews is the first comprehensive meta review to explore the scope and nature of "
         "AIEd in higher education (AIHEd) research, by synthesising secondary research (e.g., systematic "
         "reviews), indexed in the Web of Science, Scopus, ERIC, EBSCOHost, IEEE Xplore, ScienceDirect and "
         "ACM Digital Library, or captured through snowballing in OpenAlex, ResearchGate and Google Scholar. "
         "Reviews were included if they synthesised applications of AI solely in formal higher or continuing "
         "education, were published in English between 2018 and July 2023, were journal articles or full "
         "conference papers, and if they had a method section: 66 publications were included for data "
         "extraction and synthesis in EPPI Reviewer, which were predominantly systematic reviews (66.7%), "
         "published by authors from North America (27.3%), conducted in teams (89.4%) in mostly "
         "domestic-only collaborations (71.2%). Findings show that these reviews mostly focused on AIHEd "
         "generally (47.0%) or Profiling and Prediction (28.8%) as thematic foci, however key findings "
         "indicated a predominance of the use of Adaptive Systems and Personalisation in higher education. "
         "Research gaps identified suggest a need for greater ethical, methodological, and contextual "
         "considerations within future research, alongside interdisciplinary approaches to AIHEd "
         "application. Suggestions are provided to guide future primary and secondary research."),
    13: ("Purpose - The purpose of this study was to analyze importance of artificial intelligence (AI) in "
         "education and its emphasis on assessment and adversity quotient (AQ). Design/methodology/approach "
         "- The study utilizes a systematic literature review of over 141 journal papers and psychometric "
         "tests to evaluate AQ. Thematic analysis of quantitative and qualitative studies explores domains "
         "of AI in education. Findings - Results suggest that assessing the AQ of students with the help of "
         "AI techniques is necessary. Education is a vital tool to develop and improve natural intelligence, "
         "and this survey presents the discourse use of AI techniques and behavioral strategies in the "
         "education sector of the recent era. The study proposes a conceptual framework of AQ with the help "
         "of assessment style for higher education undergraduates. Originality/value - Research on AQ "
         "evaluation in the Indian context is still emerging, presenting a potential avenue for future "
         "research. Investigating the relationship between AQ and academic performance among Indian "
         "students is a crucial area of research. This can provide insights into the role of AQ in academic "
         "motivation, persistence and success in different academic disciplines and levels of education. AQ "
         "evaluation offers valuable insights into how individuals deal with and overcome challenges. The "
         "findings of this study have implications for higher education institutions to prepare for future "
         "challenges and better equip students with necessary skills for success. The papers reviewed "
         "related to AI for education opens research opportunities in the field of psychometrics, "
         "educational assessment and the evaluation of AQ."),
    16: ("The emergence of generative artificial intelligence (AI) such as ChatGPT has sparked significant "
         "assessment concerns within tertiary education. Assessment concerns have largely revolved around "
         "academic integrity issues among students, such as plagiarism and cheating. Nonetheless, it is also "
         "critical to consider that generative AI models trained on information retrieved from the Internet "
         "could produce biased and discriminatory outputs, and hallucination issues in large language models "
         "upon which generative AI acts provide made-up and untruthful outputs. This article considers the "
         "affordances and challenges of generative AI specific to assessments within tertiary education. It "
         "illustrates considerations for assessment redesign with the existence of generative AI and "
         "proposes the Against, Avoid and Adopt (AAA) principle to rethink and redesign assessments. It "
         "argues that more generative AI tools will emerge exponentially, and hence, engaging in an arms "
         "race against generative AI and policing the use of these technologies may not address the "
         "fundamental issues in assessments."),
    18: ("In the ever-evolving era of technological advancements, generative artificial intelligence (GAI) "
         "emerges as a transformative force, revolutionizing education. This review paper, guided by the "
         "PRISMA framework, presents a comprehensive analysis of GAI in education, synthesizing key insights "
         "from a selection of 207 research papers to identify research gaps and future directions in the "
         "field. This study begins with a content analysis that explores GAI's transformative impact in "
         "specific educational domains, including medical education and engineering education. The "
         "versatile applications of GAI encompass assessment, personalized learning support, and intelligent "
         "tutoring systems. Ethical considerations, interdisciplinary collaboration, and responsible "
         "technology use are highlighted, emphasizing the need for transparent GAI models and addressing "
         "biases. Subsequently, a bibliometric analysis of GAI in education is conducted, examining "
         "prominent AI tools, research focus, geographic distribution, and interdisciplinary collaboration. "
         "ChatGPT emerges as a dominant GAI tool, and the analysis reveals significant and exponential "
         "growth in GAI research in 2023. Moreover, this paper identifies promising future research "
         "directions, such as GAI-enhanced curriculum design and longitudinal studies tracking its long-term "
         "impact on learning outcomes. These findings provide a comprehensive understanding of GAI's "
         "potential in reshaping education and offer valuable insights to researchers, educators, and "
         "policymakers interested in the intersection of GAI and education."),
    19: ("An artificial intelligence-based chatbot, ChatGPT, was launched in November 2022 and is capable of "
         "generating cohesive and informative human-like responses to user input. This rapid review of the "
         "literature aims to enrich our understanding of ChatGPT's capabilities across subject domains, how "
         "it can be used in education, and potential issues raised by researchers during the first three "
         "months of its release (i.e., December 2022 to February 2023). A search of the relevant databases "
         "and Google Scholar yielded 50 articles for content analysis (i.e., open coding, axial coding, and "
         "selective coding). The findings of this review suggest that ChatGPT's performance varied across "
         "subject domains, ranging from outstanding (e.g., economics) and satisfactory (e.g., programming) "
         "to unsatisfactory (e.g., mathematics). Although ChatGPT has the potential to serve as an assistant "
         "for instructors (e.g., to generate course materials and provide suggestions) and a virtual tutor "
         "for students (e.g., to answer questions and facilitate collaboration), there were challenges "
         "associated with its use (e.g., generating incorrect or fake information and bypassing plagiarism "
         "detectors). Immediate action should be taken to update the assessment methods and institutional "
         "policies in schools and universities. Instructor training and student education are also "
         "essential to respond to the impact of ChatGPT on the educational environment."),
    20: ("According to various international reports, Artificial Intelligence in Education (AIEd) is "
         "one of the currently emerging fields in educational technology. Whilst it has been "
         "around for about 30 years, it is still unclear for educators how to make pedagogical "
         "advantage of it on a broader scale, and how it can actually impact meaningfully on "
         "teaching and learning in higher education. This paper seeks to provide an overview "
         "of research on AI applications in higher education through a systematic review. Out "
         "of 2656 initially identified publications for the period between 2007 and 2018, 146 "
         "articles were included for final synthesis, according to explicit inclusion and exclusion "
         "criteria. The descriptive results show that most of the disciplines involved in AIEd "
         "papers come from Computer Science and STEM, and that quantitative methods were "
         "the most frequently used in empirical studies. The synthesis of results presents four "
         "areas of AIEd applications in academic support services, and institutional and "
         "administrative services: 1. profiling and prediction, 2. assessment and evaluation, 3. "
         "adaptive systems and personalisation, and 4. intelligent tutoring systems. The "
         "conclusions reflect on the almost lack of critical reflection of challenges and risks of "
         "AIEd, the weak connection to theoretical pedagogical perspectives, and the need for "
         "further exploration of ethical and educational approaches in the application of AIEd "
         "in higher education."),
}

LIGATURE_MAP = {
    "ﬀ": "ff", "ﬁ": "fi", "ﬂ": "fl", "ﬃ": "ffi", "ﬄ": "ffl",
    "–": "-", "—": "-", "‘": "'", "’": "'",
    "“": '"', "”": '"', "­": "", "•": "-",
}


def normalizar(txt):
    # Palabras partidas al final de linea con guion suave (p.ej. "practi\xad tioners"
    # o "well-\xaddefined"): se unen quitando el guion suave y el espacio que
    # haya quedado en su lugar tras el aplanado de saltos de linea.
    txt = re.sub(r"­\s*", "", txt)
    for k, v in LIGATURE_MAP.items():
        txt = txt.replace(k, v)
    return txt


final = []
for i in range(1, 21):
    src = by_id[i]
    titulo, autores = META[i]
    abstract = ABSTRACT_OVERRIDES.get(i, src["abstract"])
    final.append({
        "id": i,
        "archivo": src["archivo"],
        "titulo": normalizar(titulo).strip(),
        "autores": normalizar(autores).strip(),
        "abstract": normalizar(abstract).strip(),
    })

out_path = os.path.join(BASE, "articulos.json")
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(final, f, ensure_ascii=False, indent=2)

print(f"OK -> {out_path}")
for r in final:
    print(f"{r['id']:2d} [{len(r['abstract']):4d} chars] {r['titulo'][:70]}")
