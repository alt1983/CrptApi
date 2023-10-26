
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
    private static volatile boolean watcher;

    private final String URL = "http://<server-name>[:server-port]/api/v2/{extension}/ rollout?omsId={omsId}";
    private final String CLIENT_TOKEN = "clientToken";
    private final String USER_NAME = "userName";

    //Конструктор в виде количества запросов в определенный интервал времени
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.watcher = true;
        if (requestLimit >= 0) {
            this.requestLimit = requestLimit;
            count = requestLimit;
        } else {
            throw new IllegalArgumentException("Не допустмое количество подключений");
        }
    }

    //Метод - Создание документа для ввода в оборот товара, произведенного в РФ
    public void createDocRus(Document document, String signature) {

        if(watcher == true){
            watcher = false;
            Runnable task = () -> {
                while(true) {
                    try {
                        Thread.sleep(getTimeout());
                        this.count = this.requestLimit;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread thread = new Thread(task);
            thread.start();
        }

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
            boolean busy = true;
            while(busy) {
                if (count < 0) {
                    Thread.sleep(getTimeout());
                    count--;
                }else busy = false;
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

        public String getDescription() {
            return description;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public String getDocId() {
            return docId;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public String getDocType() {
            return docType;
        }

        public String getImportRequest() {
            return importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public String getProductionType() {
            return productionType;
        }

        public String getRegDate() {
            return regDate;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setDescription(String description){
            this.description = description;
        }

        public void setImportRequest(String importRequest) {
            this.importRequest = importRequest;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        private String description;
        private final String participantInn;
        private final String docId;
        private final String docStatus;
        private final String docType;
        private String importRequest;
        private final String ownerInn;
        private final String producerInn;
        private final String productionDate;
        private final String productionType;
        private final String regDate;
        private final String regNumber;
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
            private CertificateDocument certificateDocument;
            private String certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private String productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;

            public CertificateDocument getCertificateDocument() {
                return certificateDocument;
            }

            public void setCertificateDocument(CertificateDocument certificateDocument) {
                this.certificateDocument = certificateDocument;
            }

            public void setCertificateDocumentDate(String certificateDocumentDate) {
                this.certificateDocumentDate = certificateDocumentDate;
            }

            public void setCertificateDocumentNumber(String certificateDocumentNumber) {
                this.certificateDocumentNumber = certificateDocumentNumber;
            }

            public void setOwnerInn(String ownerInn) {
                this.ownerInn = ownerInn;
            }

            public void setProducerInn(String producerInn) {
                this.producerInn = producerInn;
            }

            public void setProductionDate(String productionDate) {
                this.productionDate = productionDate;
            }

            public void setTnvedCode(String tnvedCode) {
                this.tnvedCode = tnvedCode;
            }

            public void setUitCode(String uitCode) {
                this.uitCode = uitCode;
            }

            public void setUituCode(String uituCode) {
                this.uituCode = uituCode;
            }

            public String getCertificateDocumentDate() {
                return certificateDocumentDate;
            }

            public String getCertificateDocumentNumber() {
                return certificateDocumentNumber;
            }

            public String getOwnerInn() {
                return ownerInn;
            }

            public String getProducerInn() {
                return producerInn;
            }

            public String getProductionDate() {
                return productionDate;
            }

            public String getTnvedCode() {
                return tnvedCode;
            }

            public String getUitCode() {
                return uitCode;
            }

            public String getUituCode() {
                return uituCode;
            }

            public enum CertificateDocument {
                CONFORMITY_CERTIFICATE, CONFORMITY_DECLARATION
            }

        }
    }
}