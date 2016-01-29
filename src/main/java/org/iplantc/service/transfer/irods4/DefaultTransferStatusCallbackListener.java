package org.iplantc.service.transfer.irods4;

import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.transfer.TransferStatus;
import org.irods.jargon.core.transfer.TransferStatusCallbackListener;

public class DefaultTransferStatusCallbackListener implements TransferStatusCallbackListener {

    private Throwable transferError = null;
    @Override
    public FileStatusCallbackResponse statusCallback(TransferStatus transferStatus)
            throws JargonException {
        if (transferStatus.getTransferException() != null) {
            transferError = transferStatus.getTransferException();
        }
        return FileStatusCallbackResponse.CONTINUE;
    }

    @Override
    public void overallStatusCallback(TransferStatus transferStatus) throws JargonException {
        // nothing to see here
    }

    @Override
    public CallbackResponse transferAsksWhetherToForceOperation(String irodsAbsolutePath, boolean isCollection) {
        return CallbackResponse.YES_FOR_ALL;
    }
    
    /**
     * @return true if there were no errors.
     */
    public boolean hasErrors() {
        return transferError != null;
    }

    /**
     * @return the transferError
     */
    public Throwable getTransferError() {
        return transferError;
    }

    /**
     * @param transferError the transferError to set
     */
    public void setTransferError(Throwable transferError) {
        this.transferError = transferError;
    }

}
