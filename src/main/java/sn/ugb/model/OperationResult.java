package sn.ugb.model;

/**
 * Résultat d'une opération de signature ou vérification.
 * Retourné en JSON par le contrôleur REST.
 */
public class OperationResult {

    private boolean success;
    private String  message;
    private String  detail;
    private long    durationMs;
    private String  outputPath;

    public OperationResult() {}

    public OperationResult(
            boolean success,
            String message,
            String detail,
            long durationMs) {
        this.success    = success;
        this.message    = message;
        this.detail     = detail;
        this.durationMs = durationMs;
    }

    // Getters et Setters
    public boolean isSuccess()    { return success; }
    public String  getMessage()   { return message; }
    public String  getDetail()    { return detail; }
    public long    getDurationMs(){ return durationMs; }
    public String  getOutputPath(){ return outputPath; }

    public void setSuccess(boolean v)   { success = v; }
    public void setMessage(String v)    { message = v; }
    public void setDetail(String v)     { detail = v; }
    public void setDurationMs(long v)   { durationMs = v; }
    public void setOutputPath(String v) { outputPath = v; }
}