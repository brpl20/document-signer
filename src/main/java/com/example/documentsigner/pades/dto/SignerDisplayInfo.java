package com.example.documentsigner.pades.dto;

import java.util.Date;

/**
 * Information extracted from certificate for display in visual signature.
 */
public class SignerDisplayInfo {
    private String name;
    private String cpf;
    private String organization;
    private String issuerCA;
    private Date signingTime;

    public SignerDisplayInfo() {
    }

    public SignerDisplayInfo(String name, String cpf, String organization, String issuerCA, Date signingTime) {
        this.name = name;
        this.cpf = cpf;
        this.organization = organization;
        this.issuerCA = issuerCA;
        this.signingTime = signingTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getIssuerCA() {
        return issuerCA;
    }

    public void setIssuerCA(String issuerCA) {
        this.issuerCA = issuerCA;
    }

    public Date getSigningTime() {
        return signingTime;
    }

    public void setSigningTime(Date signingTime) {
        this.signingTime = signingTime;
    }

    /**
     * Get formatted CPF with masking for privacy: ***.***. ***-XX
     */
    public String getMaskedCpf() {
        if (cpf == null || cpf.length() < 11) {
            return cpf;
        }
        String digits = cpf.replaceAll("[^0-9]", "");
        if (digits.length() >= 11) {
            return "***.***.***-" + digits.substring(9, 11);
        }
        return cpf;
    }
}
