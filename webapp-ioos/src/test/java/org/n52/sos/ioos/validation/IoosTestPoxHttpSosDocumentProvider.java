package org.n52.sos.ioos.validation;

import java.net.MalformedURLException;
import java.net.URL;

import org.n52.sos.binding.BindingConstants;

import com.axiomalaska.ioos.sos.validator.exception.InvalidRequestConfigurationException;
import com.axiomalaska.ioos.sos.validator.exception.InvalidUrlException;
import com.axiomalaska.ioos.sos.validator.provider.http.PoxHttpSosDocumentProvider;
import com.axiomalaska.ioos.sos.validator.provider.http.config.RequestConfiguration;

public class IoosTestPoxHttpSosDocumentProvider extends PoxHttpSosDocumentProvider{
    public IoosTestPoxHttpSosDocumentProvider() throws InvalidUrlException, MalformedURLException,
            InvalidRequestConfigurationException {
        super(new URL(JettyHelper.getJettyUrl() + BindingConstants.POX_BINDING_ENDPOINT),
                RequestConfiguration.exampleConfig());
    }
}