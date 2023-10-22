import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

public class CrptApi {

    private int requestLimit;
    private final TimeUnit timeUnit;
    private static volatile int count;

    private final String URL = "http://<server-name>[:server-port]/api/v2/{extension}/ rollout?omsId={omsId}";
    private final String CLIENT_TOKEN = "clientToken";
    private final String USER_NAME = "userName";

    //Конструктор в виде количества запросов в определенный интервал времени
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        if (requestLimit >= 0) {
            this.requestLimit = requestLimit;
            count = requestLimit;
        } else {
            throw new IllegalArgumentException("Не допустмое количество подключений");
        }
    }

    //Метод - Создание документа для ввода в оборот товара, произведенного в РФ
    public void createDocRus(Document document, String signature) {
        httpRequest(getDoc(document, signature).toString(), signature);
    }

    private JSONObject getDoc(Document document, String signature) {
        JSONObject doc = new JSONObject();
        if (document.getDescription() != null) {
            doc.put("description", document.getProducerInn());
        }
        doc.put("doc_id", document.getDocId());
        doc.put("doc_status", document.getDocStatus());
        doc.put("doc_type", document.getDocType());
        if (document.getImportRequest() != null) {
            doc.put("importRequest", document.getImportRequest());
        }
        doc.put("owner_inn", document.getOwnerInn());
        doc.put("participant_inn", document.getParticipantInn());
        doc.put("producer_inn", document.getProducerInn());
        doc.put("production_date", document.getProducerInn());
        doc.put("production_type", document.getProductionType());
        List<Document.Product> products = document.getProducts();
        if (products != null) {
            JSONArray productsJList = new JSONArray();
            for(Document.Product product: products) {
                JSONObject productJ = new JSONObject();
                if (product.getCertificateDocument() != null) {
                    productJ.put("certificate_document", product.getCertificateDocument());
                } else if (product.getCertificateDocumentDate() != null) {
                    productJ.put("certificate_document_date", product.getCertificateDocumentDate());
                } else if (product.getCertificateDocumentNumber() != null) {
                    productJ.put("certificate_document_number", product.getCertificateDocumentNumber());
                }
                productJ.put("owner_inn", product.getOwnerInn());
                productJ.put("producer_inn", product.getProducerInn());
                productJ.put("production_date", document.getProductionDate());
                if (!document.getProductionDate().equals(product.getProductionDate())) {
                    productJ.put("production_date", product.getProductionDate());
                }
                productJ.put("tnved_code", product.getTnvedCode());
                if (product.getUitCode() != null) {
                    productJ.put("uit_code", product.getUitCode());
                } else if (product.getUituCode() != null) {
                    productJ.put("uitu_code", product.getUituCode());
                } else {
                    throw new IllegalArgumentException("Поле uit_code или uitu_code обязательно");
                }
                productsJList.add(products);
            }
            doc.put("products", productsJList);
        }
        doc.put("reg_date", document.getRegDate());
        doc.put("reg_number", document.getRegNumber());
        return doc;
    }

    private void httpRequest(String json, String signature) {
                count--;
        try {
                if (count < 0) {
                    Thread.sleep(getTimeout());
                    count = requestLimit;
                    count--;
                }
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(URL);
            StringEntity entity = new StringEntity(json);
            post.addHeader("Content-Type", "application/json");
            post.addHeader("clientToken", CLIENT_TOKEN);
            post.addHeader("userName", USER_NAME);
            post.addHeader("signature", signature);
            post.setEntity(entity);
            HttpResponse response = httpClient.execute(post);
            StatusLine statusLine = response.getStatusLine();
            if(statusLine.getStatusCode() != 200) {
                throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
            }
            httpClient.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public enum TimeUnit {
        SECOND, MINUTE, HOUR
    }

    private long getTimeout() {
        if (timeUnit == TimeUnit.SECOND) return 1000;
        if (timeUnit == TimeUnit.MINUTE) return 60*1000;
        if (timeUnit == TimeUnit.HOUR) return 3600*1000;
        return 0;
    }

    //Класс с описанием параметров документа
    public static class Document {
        @Getter
        private String description;

        @Getter
        private final String participantInn;

        @Getter
        private final String docId;

        @Getter
        private final String docStatus;

        @Getter
        private final String docType;

        @Getter
        @Setter
        private String importRequest;

        @Getter
        private final String ownerInn;

        @Getter
        private final String producerInn;

        @Getter
        private final String productionDate;

        @Getter
        private final String productionType;

        @Getter
        private final String regDate;

        @Getter
        private final String regNumber;

        @Getter
        @Setter
        private List<Product> products;

        public Document(String participantInn, String docId, String docStatus,
                        String docType, String ownerInn, String producerInn,
                        String productionDate, String productionType,
                        String regDate, String regNumber) {
            this.participantInn = participantInn;
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.regDate = regDate;
            this.regNumber = regNumber;
        }

        public static class Product {
            @Getter
            @Setter
            private CertificateDocument certificateDocument;

            @Getter
            @Setter
            private String certificateDocumentDate;

            @Getter
            @Setter
            private String certificateDocumentNumber;

            @Getter
            @Setter
            private String ownerInn;

            @Getter
            @Setter
            private String producerInn;

            @Getter
            @Setter
            private String productionDate;

            @Getter
            @Setter
            private String tnvedCode;

            @Getter
            @Setter
            private String uitCode;

            @Getter
            @Setter
            private String uituCode;

            public enum CertificateDocument {
                CONFORMITY_CERTIFICATE, CONFORMITY_DECLARATION
            }

        }
    }
}
