package com.nico.turnos.dto;

public class PasswordUpdateRequest {
    private String currentPassword;
    private String newPassword;
    private String verificationCode; // <--- NUEVO CAMPO

    // Getters y Setters
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
}