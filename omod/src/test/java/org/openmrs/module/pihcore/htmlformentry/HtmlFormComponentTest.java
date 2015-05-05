package org.openmrs.module.pihcore.htmlformentry;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentryui.HtmlFormUtil;
import org.openmrs.module.metadatadeploy.api.MetadataDeployService;
import org.openmrs.module.pihcore.PihCoreActivator;
import org.openmrs.module.pihcore.config.Config;
import org.openmrs.module.pihcore.config.ConfigDescriptor;
import org.openmrs.module.pihcore.setup.HtmlFormSetup;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;

import java.io.InputStream;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SkipBaseSetup
public class HtmlFormComponentTest extends BaseModuleContextSensitiveTest {

    @Autowired
    private MetadataDeployService metadataDeployService;

    private PihCoreActivator pihCoreActivator;

    @Override
    public Properties getRuntimeProperties() {
        Properties p = super.getRuntimeProperties();
        p.setProperty("pih.config", "pihcore");
        return p;
    }

    @Before
    public void beforeEachTest() throws Exception {
        initializeInMemoryDatabase();
        executeDataSet("requiredDataTestDataset.xml");
        authenticate();

        // set up metatdata from pih core first
        pihCoreActivator = new PihCoreActivator();
        Config config = mock(Config.class);
        when(config.getCountry()).thenReturn(ConfigDescriptor.Country.HAITI);
        when(config.getSite()).thenReturn(ConfigDescriptor.Site.MIREBALAIS);
        pihCoreActivator.setConfig(config);
        pihCoreActivator.started();

        // load the test bundle of MDS concepts
        metadataDeployService.installBundle(Context.getRegisteredComponents(ConceptsFromMetadataSharing.class).get(0));

        HtmlFormSetup.setupHtmlFormEntryTagHandlers();
    }

    @Test
    public void testHtmlForms() throws Exception {
        String[] formsToTest = {
                "deathCertificate.xml",
                "patientRegistration.xml",
                "patientRegistration-rs.xml",
                "patientRegistration-social.xml",
                "haiti/primary-care-adult-history.xml",
                "haiti/primary-care-adult-exam-dx.xml"
        };

        for (String formName : formsToTest) {
            // loading up all of the UI Framework beans to fetch this resource is problematic, so just get it directly:
            InputStream in = getClass().getClassLoader().getResourceAsStream("web/module/resources/htmlforms/" + formName);
            String xml = IOUtils.toString(in);
            IOUtils.closeQuietly(in);

            HtmlForm form = HtmlFormUtil.getHtmlFormFromResourceXml(Context.getFormService(), Context.getService(HtmlFormEntryService.class), xml);

            FormEntrySession fes = new FormEntrySession(new Patient(), form, FormEntryContext.Mode.ENTER, new MockHttpSession());
            try {
                String html = fes.getHtmlToDisplay();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                Assert.fail("Failed to load " + formName + ": " + ex.getMessage());
            }
        }
    }


}