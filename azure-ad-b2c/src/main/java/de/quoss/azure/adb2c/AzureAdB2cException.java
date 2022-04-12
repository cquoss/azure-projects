package de.quoss.azure.adb2c;

public class AzureAdB2cException extends Exception {

    public AzureAdB2cException(final String s) {
        super(s);
    }

    public AzureAdB2cException(final Throwable t) {
        super(t);
    }

    public AzureAdB2cException(final String s, final Throwable t) {
        super(s, t);
    }

}
