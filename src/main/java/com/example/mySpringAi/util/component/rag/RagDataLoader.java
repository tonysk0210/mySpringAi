package com.example.mySpringAi.util.component.rag;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagDataLoader {

    private final VectorStore vectorStore;
    private final VectorStore pdfVectorSotre;

    @Value("classpath:/Eazybytes_HR_Policies.pdf")
    Resource pdfFile;

    /**
     * vectorStore     -> Spring AI 自動建立 -> rag-collection
     * pdfVectorStore  -> 你自己建立       -> pdf-collection
     * <p>
     * vectorStore 來自 pom.xml 的 spring-ai-starter-vector-store-qdrant
     * 並透過 spring.ai.vectorstore.qdrant.collection-name=rag-collection 決定 collection
     */
    @Autowired
    public RagDataLoader(VectorStore vectorStore, @Qualifier("pdfVectorStore") VectorStore pdfVectorSotre) {

        this.vectorStore = vectorStore; // 自動建立的 vectorStore
        this.pdfVectorSotre = pdfVectorSotre; // 手動建立的 pdfVectorStore
        /*
        第一個參數：
        VectorStore vectorStore
          -> 找 VectorStore 型別
          -> 找到 vectorStore、pdfVectorStore 兩個
          -> 沒有 @Qualifier
          -> 用參數名稱 vectorStore 去 match bean 名稱
          -> 選到 auto-config 的 vectorStore

        第二個參數：
        @Qualifier("pdfVectorStore") VectorStore pdfVectorSotre
          -> 直接選 pdfVectorStore
        */
    }

    /**
     * 載入「手寫句子資料」到預設 rag-collection。
     */
    @PostConstruct
    public void loadSentenceIntoVectorStore() {
        List<String> sentences = List.of(
                "Java is used for building scalable enterprise applications.",
                "Python is commonly used for machine learning and automation tasks.",
                "JavaScript is essential for creating interactive web pages.",
                "Docker packages applications into lightweight containers.",
                "Kubernetes automates container orchestration at scale.",
                "Redis is an in-memory data store used for caching.",
                "PostgreSQL supports complex queries and full ACID compliance.",
                "Kafka is a distributed event streaming platform.",
                "REST APIs allow stateless client-server communication.",
                "GraphQL enables clients to fetch exactly the data they need.",
                "Credit scores influence the interest rates on loans.",
                "Mutual funds pool money from investors to buy securities.",
                "Bitcoin operates on a decentralized peer-to-peer network.",
                "Ethereum supports smart contract deployment.",
                "The stock market opens at 9:30 a.m. EST on weekdays.",
                "Compound interest increases investment returns over time.",
                "Diversifying investments reduces overall risk.",
                "A blockchain is a distributed, immutable ledger of transactions.",
                "Photosynthesis is how plants convert sunlight into energy.",
                "The water cycle involves evaporation, condensation, and precipitation.",
                "The ozone layer protects Earth from harmful ultraviolet rays.",
                "Earth revolves around the Sun in an elliptical orbit.",
                "Lightning is a discharge of electricity caused by charged clouds.",
                "DNA is the molecule that carries genetic instructions in living organisms.",
                "Volcanoes form when magma rises through Earth's crust.",
                "Earthquakes are caused by sudden tectonic shifts.",
                "The Sahara is the largest hot desert in the world.",
                "Mount Kilimanjaro is the tallest mountain in Africa.",
                "Japan is known for its cherry blossoms and advanced technology.",
                "The Great Wall of China is over 13,000 miles long.",
                "Niagara Falls is located between Canada and the U.S.",
                "The Amazon River is the second longest river in the world.",
                "Oats are high in fiber and help reduce cholesterol.",
                "Drinking water improves digestion and skin health.",
                "A balanced diet includes proteins, carbs, fats, and vitamins.",
                "Broccoli is rich in vitamins A, C, and K.",
                "Green tea contains antioxidants beneficial for metabolism.",
                "Too much sugar increases the risk of diabetes.",
                "Walking 30 minutes a day improves cardiovascular health.",
                "Meditation can reduce stress and improve focus.",
                "Gratitude journaling is linked to higher happiness levels.",
                "Deep breathing exercises help regulate anxiety.",
                "Reading daily improves vocabulary and cognitive function.",
                "Setting daily goals increases productivity.",
                "STEM stands for Science, Technology, Engineering, and Mathematics.",
                "Bloom's taxonomy categorizes educational goals.",
                "Project-based learning enhances student engagement.",
                "Online courses offer flexibility for remote learners.",
                "Flashcards are effective for memorizing vocabulary.",
                "Agile methodology promotes iterative software development.",
                "OKRs help align team goals with business strategy.",
                "Remote work offers flexibility but requires clear communication.",
                "CRM systems manage customer relationships and sales pipelines.",
                "SWOT analysis identifies strengths, weaknesses, opportunities, and threats."
        );

        // 1. 把句子轉成 Document
        List<Document> documents = sentences.stream().map(Document::new).toList(); // VectorStore 只吃 Document，不吃 String

        // 2. 把 Document 送進 Qdrant
        vectorStore.add(documents); // 把資料送進 Qdrant
    }

    /**
     * 載入「PDF 檔案資料」到自定義 pdf-collection。
     */
    @PostConstruct
    public void loadPdfIntoVectorStore() {

        // 1. 建立一個 TikaDocumentReader 讀取 pdfFile，並嘗試從 PDF 中抽出文字內容
        TikaDocumentReader reader = new TikaDocumentReader(pdfFile);

        // 2. 真正讀取 PDF，並把結果轉成 Spring AI 的 Document 清單
        List<Document> documents = reader.get();

        // 3. 確認 PDF 是否有被成功讀取 Document size: 1
        System.out.println("Document size: " + documents.size());

        // 4. 使用 TokenTextSplitter，用 token 數量來切文件：每個 chunk 目標大小約為 200 tokens，最多切 400 個 chunk
        TextSplitter splitter = TokenTextSplitter.builder().withChunkSize(200).withMaxNumChunks(400).build();

        // 5. 把切好的 Document 清單送進 pdfVectorSotre，完成建立 pdf-collection
        pdfVectorSotre.add(splitter.split(documents));
    }
}
