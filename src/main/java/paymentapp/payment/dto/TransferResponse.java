package paymentapp.payment.dto;

import lombok.Data;

@Data
public class TransferResponse {
    private String txId;
    private String status;
    private String message;
    private Long timestamp;
    
    public static TransferResponse accepted(String txId) {
        TransferResponse response = new TransferResponse();
        response.setTxId(txId);
        response.setStatus("ACCEPTED");
        response.setMessage("Transfer request accepted for processing");
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
    
    public static TransferResponse rejected(String reason) {
        TransferResponse response = new TransferResponse();
        response.setStatus("REJECTED");
        response.setMessage(reason);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }
}