/*******************************************************************************
 * Copyright (c) 2015, 2016 Substance Abuse and Mental Health Services Administration (SAMHSA)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Eversolve, LLC - initial IExHub implementation for Health Information Exchange (HIE) integration
 *     Anthony Sute, Ioana Singureanu
 *******************************************************************************/
package org.iexhub.services.test;


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IResource;
import ca.uhn.fhir.model.dstu2.composite.AttachmentDt;
import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu2.composite.PeriodDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Basic;
import ca.uhn.fhir.model.dstu2.resource.Contract;
import ca.uhn.fhir.model.dstu2.resource.Contract.Rule;
import ca.uhn.fhir.model.dstu2.resource.ListResource;
import ca.uhn.fhir.model.dstu2.resource.Organization;
import ca.uhn.fhir.model.dstu2.resource.Organization.Contact;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.Practitioner;
import ca.uhn.fhir.model.dstu2.valueset.AddressTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.AdministrativeGenderEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointSystemEnum;
import ca.uhn.fhir.model.dstu2.valueset.ContactPointUseEnum;
import ca.uhn.fhir.model.dstu2.valueset.ListModeEnum;
import ca.uhn.fhir.model.dstu2.valueset.ListStatusEnum;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;

public class PrivacyConsentTest {
	// FHIR resource identifiers for contained objects
	private static String consentId = "consentId";
	private static String patientId = "patientId";
	private static String sourceOrganizationId = "sourceOrgOID";
	private static String sourcePractitionerId = "sourcePractitionerNPI";
	private static String recipientPractitionerId = "recipientPractitionerNPI";
	private static String testResourcesPath = "src/test/resources/";
	private static FhirContext ctxt = new FhirContext();
	private static Contract consentAsContract;

	private static String uriPrefix = "urn:oid:";
	private static String iExHubDomainOid = "2.16.840.1.113883.3.72.5.9.1";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		// Consent as contract
		consentAsContract = new Contract();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws IOException {
		String currentTest = "PrivacyConsentTest";
		// FHIR objects used to create a Consent
		Patient testPatientResource = new Patient();
		Organization sourceOrganizationResource = new Organization();
		Contact organizationContactResource = new Contact();
		Practitioner sourcePractitionerResource = new Practitioner();
		Practitioner recipientPractitionerResource = new Practitioner();

		// create the testPatient resource to be embedded into a contract
		testPatientResource.setId(new IdDt(patientId));
		testPatientResource.addName().addFamily("Patient Family Name").addGiven("Patient Given Name");
		// set SSN value using coding system 2.16.840.1.113883.4.1
		testPatientResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.1").setValue("123-45-6789");
		// set local patient id
		testPatientResource.addIdentifier().setSystem(uriPrefix + iExHubDomainOid).setValue(patientId);
		testPatientResource.setGender(AdministrativeGenderEnum.FEMALE);
		testPatientResource.setBirthDate(new DateDt("1966-10-22"));
		testPatientResource.addAddress().addLine("Patient Address Line").setCity("City").setState("NY")
				.setPostalCode("12345").setType(AddressTypeEnum.POSTAL);
		testPatientResource.addTelecom().setUse(ContactPointUseEnum.HOME).setSystem(ContactPointSystemEnum.PHONE)
				.setValue("555-1212");
		testPatientResource.addTelecom().setUse(ContactPointUseEnum.HOME).setSystem(ContactPointSystemEnum.EMAIL)
				.setValue("patient@home.org");
		// Provider organization...
		// set id to be used to reference the providerOrganization as an inline resource
		sourceOrganizationResource.setId(new IdDt(sourceOrganizationId));
		// system 2.16.840.1.113883.4.6
		sourceOrganizationResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6")
				.setValue("NPI of source organization");
		sourceOrganizationResource.setName("Source Organization Name");
		sourceOrganizationResource.addAddress().addLine("1 Source Drive").setCity("Source City").setState("NY")
				.setPostalCode("01221");
		// contact
		organizationContactResource.addTelecom().setUse(ContactPointUseEnum.WORK)
				.setSystem(ContactPointSystemEnum.PHONE).setValue("408-555-1212");
		organizationContactResource.addTelecom().setUse(ContactPointUseEnum.WORK)
				.setSystem(ContactPointSystemEnum.EMAIL).setValue("contact@sourceorgnization.org");
		HumanNameDt contactName = new HumanNameDt();
		contactName.addFamily().setValue("Contact Family Name");
		contactName.addGiven().setValue("Contact Given Name");
		organizationContactResource.setName(contactName);
		List<Contact> contacts = new ArrayList<Contact>();
		contacts.add(organizationContactResource);
		sourceOrganizationResource.setContact(contacts);
		// set reference using "#" prefix
		testPatientResource.addCareProvider().setReference("#" + sourceOrganizationId);

		// authoring practitioner
		sourcePractitionerResource.setId(new IdDt(sourcePractitionerId));
		sourcePractitionerResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6")
				.setValue("NPI of source provider");
		sourcePractitionerResource.getName().addFamily("Source Practitioner Last Name")
				.addGiven("Source Practitioner Given Name").addSuffix("MD");
		sourcePractitionerResource.addAddress().addLine("Source Practitioner Address Line").setCity("City")
				.setState("NY").setPostalCode("98765");
		sourcePractitionerResource.addTelecom().setSystem(ContactPointSystemEnum.PHONE).setValue("212-555-1212");
		sourcePractitionerResource.addTelecom().setUse(ContactPointUseEnum.WORK).setSystem(ContactPointSystemEnum.EMAIL)
				.setValue("contact@sourceorgnization.org");
		// recipient practitioner
		recipientPractitionerResource.setId(new IdDt(recipientPractitionerId));
		recipientPractitionerResource.addIdentifier().setSystem(uriPrefix + "2.16.840.1.113883.4.6")
				.setValue("NPI of recipient provider");
		recipientPractitionerResource.getName().addFamily("Recipient Practitioner Last Name")
				.addGiven("Recipient Practitioner Given Name").addSuffix("MD").addPrefix("Ms.");
		recipientPractitionerResource.addAddress().addLine("Recipient Practitioner Address Line").setCity("City")
				.setState("NY").setPostalCode("98765");
		recipientPractitionerResource.addTelecom().setUse(ContactPointUseEnum.WORK)
				.setSystem(ContactPointSystemEnum.PHONE).setValue("212-000-1212");
		recipientPractitionerResource.addTelecom().setUse(ContactPointUseEnum.WORK)
				.setSystem(ContactPointSystemEnum.EMAIL).setValue("recipient@destination.org");
		// populate contract
		// set the id as a concatenated "OID.consentId"
		consentAsContract.setId(new IdDt(iExHubDomainOid + "." + consentId));

		consentAsContract.getIdentifier().setSystem(uriPrefix + iExHubDomainOid).setValue("consent GUID");
		consentAsContract.getType().addCoding().setSystem("urn:oid:2.16.840.1.113883.5.4").setCode("IDSCL");
		consentAsContract.getActionReason()
				.add(new CodeableConceptDt("http://hl7.org/fhir/contractsubtypecodes", "TREAT"));

		DateTimeDt issuedDateTime = new DateTimeDt();
		issuedDateTime.setValue(Calendar.getInstance().getTime());
		consentAsContract.setIssued(issuedDateTime);
		// specify the covered entity authorized to disclose
		// add source resource and authority reference
		consentAsContract.getContained().getContainedResources().add(sourceOrganizationResource);
		consentAsContract.addAuthority().setReference("#" + sourceOrganizationId);
		/* This is required if the organization was not already added as a "contained" resource reference by the Patient
		   contract.getContained().getContainedResources().add(sourceOrganizationResource);
		   specify the provider who authored the data */
		consentAsContract.addActor().getEntity().setReference("#" + sourcePractitionerId);
		consentAsContract.getContained().getContainedResources().add(sourcePractitionerResource);
		// specify the patient identified in the consent
		// add local reference to patient
		ResourceReferenceDt patientReference = new ResourceReferenceDt("#" + patientId);
		consentAsContract.getSubject().add(patientReference);
		consentAsContract.getSignerFirstRep()
				.setType(new CodingDt("http://hl7.org/fhir/contractsignertypecodes", "1.2.840.10065.1.12.1.7"));

		consentAsContract.getSignerFirstRep()
				.setSignature(testPatientResource.getNameFirstRep().getNameAsSingleString());
		consentAsContract.getSignerFirstRep().setParty(patientReference);
		// add test patient as a contained resource rather than external reference
		consentAsContract.getContained().getContainedResources().add(testPatientResource);
		// set terms of consent and intended recipient(s)
		PeriodDt applicablePeriod = new PeriodDt();
		applicablePeriod.setEnd(new DateTimeDt("2016-10-10"));
		applicablePeriod.setStart(new DateTimeDt("2015-10-10"));
		consentAsContract.getTermFirstRep().setApplies(applicablePeriod);
		// list all recipients
		consentAsContract.getTermFirstRep().addActor().getEntity().setReference("#" + recipientPractitionerId);
		consentAsContract.getContained().getContainedResources().add(recipientPractitionerResource);
		consentAsContract.getTermFirstRep().setText("description of the consent terms");
		// add granular preferences
		String includedDataListId = "includedListOfDataTypes";
		ListResource list = new ListResource();
		list.setId(new IdDt(includedDataListId));
		list.setTitle("List of included data types");
		// specifies how the list items are to be used
		CodeableConceptDt includeCodeValue = new CodeableConceptDt("http://hl7.org/fhir/v3/SetOperator", "I");
		includeCodeValue.setText("Include");
		list.setCode(includeCodeValue);
		list.setStatus(ListStatusEnum.CURRENT);
		list.setMode(ListModeEnum.SNAPSHOT_LIST);

		// add discharge summary document type
		ListResource.Entry dischargeSummaryEntry = new ListResource.Entry();
		// use list item flag to specify a category and the item to specify an instance (e.g. DocumentReference)
		CodeableConceptDt dischargeSummaryCode = new CodeableConceptDt("urn:oid:2.16.840.1.113883.5.25", "SDV");
		// dischargeSummaryCode
		dischargeSummaryCode.setText("Sexual and domestic violence related");
		Basic basicItem1 = new Basic();
		basicItem1.setId(new IdDt("item1"));
		basicItem1.setCode(dischargeSummaryCode);

		ResourceReferenceDt itemReference1 = new ResourceReferenceDt("#item1");
		dischargeSummaryEntry.setItem(itemReference1);
		list.addEntry(dischargeSummaryEntry);
		// add a summary note document type
		ListResource.Entry summaryNoteEntry = new ListResource.Entry();
		/* "34133-9" LOINC term is currently used as the Clinical Document code for both
		   the Care Record Summary (CRS) and Continuity of Care Document (CCD). */
		CodeableConceptDt summaryNoteCode = new CodeableConceptDt("urn:oid:2.16.840.1.113883.5.25", "PSY");
		summaryNoteCode.setText("Psychiatry Related Data");
		summaryNoteEntry.setDeleted(false);
		Basic basicItem2 = new Basic();
		basicItem2.setId("item2");
		basicItem2.setCode(summaryNoteCode);
		ResourceReferenceDt itemReference2 = new ResourceReferenceDt("#item2");
		itemReference2.setDisplay("referenced document type or instance");
		summaryNoteEntry.setItem(itemReference2);
		list.addEntry(summaryNoteEntry);

		// substance abuse category may reference a single category code or several diagnosis codes
		ListResource.Entry substanceAbuseRelatedEntry = new ListResource.Entry();
		CodeableConceptDt substanceAbuseRelatedCode = new CodeableConceptDt("urn:oid:2.16.840.1.113883.5.25", "ETH");
		substanceAbuseRelatedCode.setText("Substance Abuse Related Data");
		Basic basicItem3 = new Basic();
		basicItem3.setId("item3");
		basicItem3.setCode(substanceAbuseRelatedCode);

		ResourceReferenceDt itemReference3 = new ResourceReferenceDt("#item3");
		substanceAbuseRelatedEntry.setItem(itemReference3);
		list.addEntry(substanceAbuseRelatedEntry);
		// add list to contract
		consentAsContract.getTerm().get(0).getSubject().setReference("#" + includedDataListId);
		consentAsContract.getContained().getContainedResources().add(list);
		// add items as Basic resources
		consentAsContract.getContained().getContainedResources().add(basicItem1);
		consentAsContract.getContained().getContainedResources().add(basicItem2);
		consentAsContract.getContained().getContainedResources().add(basicItem3);

		// Use the narrative generator
		// @TODO: add generator Thymeleaf templates
		// Create XML and JSON files including generated narrative XHTML
		String xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true)
				.encodeResourceToString(consentAsContract);
		File xmlEncodedGranularConsentFile = new File(testResourcesPath + "/XML/temp/" + currentTest + ".xml");
		try {
			FileUtils.writeStringToFile(xmlEncodedGranularConsentFile, xmlEncodedGranularConsent);
		} catch (IOException e) {
			fail("Write Consent resource to XML:" + e.getMessage());
		}
		// transform consent to XACML and add XACML to the Contract.Rule
		File xformFile = new File(testResourcesPath + "/XSLT/" + "FHIR2XACML.xsl");
		TransformerFactory factory = TransformerFactory.newInstance();
		Source xform = new StreamSource(xformFile);
		Transformer transformer;
		try {
			transformer = factory.newTransformer(xform);
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");	
			Source consentResourceSource = new StreamSource(xmlEncodedGranularConsentFile);
			File testXacmlFile = new File(testResourcesPath + "/XML/temp/xacml.xml");
	        transformer.transform(consentResourceSource, new StreamResult(testXacmlFile));
	        String xacmlPolicyStr = FileUtils.readFileToString(testXacmlFile, "UTF-8");
			// Add the XACML policy to Consent resource
			Rule xacmlPolicyRule = new Rule();
			AttachmentDt xacmlAttachment = new AttachmentDt();
			xacmlAttachment.setData(xacmlPolicyStr.getBytes());
			xacmlPolicyRule.setContent(xacmlAttachment);
			consentAsContract.addRule(xacmlPolicyRule);
		} catch (Exception e1) {
			fail("Create XACML policy from Consent: " + e1.getMessage());
			e1.printStackTrace();
		}

		// write Consent + XACML
		xmlEncodedGranularConsent = ctxt.newXmlParser().setPrettyPrint(true).encodeResourceToString(consentAsContract);
		try {
			FileUtils.writeStringToFile(xmlEncodedGranularConsentFile, xmlEncodedGranularConsent);
		} catch (IOException e) {
			fail("Write resource to XML + XACML:" + e.getMessage());
		}

		String jsonEncodedGranularConsent = ctxt.newJsonParser().setPrettyPrint(true)
				.encodeResourceToString(consentAsContract);

		try {
			FileUtils.writeStringToFile(new File(testResourcesPath + "/JSON/temp/" + currentTest + ".json"),
					jsonEncodedGranularConsent);
		} catch (IOException e) {
			fail("Write resource to JSON:" + e.getMessage());
		}
		Contract consentFromFile = null;
		String readContractString = "";
		try {
			readContractString = FileUtils
					.readFileToString(new File(testResourcesPath + "/XML/temp/" + currentTest + ".xml"), "UTF-8");
		} catch (IOException e) {
			fail("Reading resource from file error:" + e.getMessage());
		}
		consentFromFile = (Contract) ctxt.newXmlParser().parseResource(readContractString);
		if (!consentFromFile.getActionReason().isEmpty()) {
			// read purpose of use
			Iterator<CodeableConceptDt> purposeOfUseIterator = consentFromFile.getActionReason().iterator();
			while (purposeOfUseIterator.hasNext()) {
				CodeableConceptDt cd = purposeOfUseIterator.next();
				assertTrue(cd.getCodingFirstRep().getCode().equalsIgnoreCase("TREAT"));
			}
		}
		if (!consentFromFile.getSubject().isEmpty()) {
			ResourceReferenceDt consentSubjectRef = consentFromFile.getSubject().get(0);
			IBaseResource referencedSubject = consentSubjectRef.getResource();
			String referencedId = referencedSubject.getIdElement().getIdPart();
			assertTrue(referencedId.equalsIgnoreCase(patientId));
			ListIterator<IResource> containedResources = consentFromFile.getContained().getContainedResources()
					.listIterator();
			Patient subjectPatientResource = null;
			while (containedResources.hasNext()) {
				IResource cr = (IResource) containedResources.next();
				if (cr.getIdElement().getIdPart().equalsIgnoreCase(referencedId)) {
					subjectPatientResource = (Patient) cr;
					HumanNameDt n = subjectPatientResource.getName().get(0);
					assertTrue(n.getFamily().get(0).getValue().equalsIgnoreCase("Patient Family Name"));
					break;
				}
			}

			// read XACML from Contract.Rule
			Rule xacmlFromFileRule = consentFromFile.getRule().get(0);
			AttachmentDt xacmlPolicyAttachment = (AttachmentDt) xacmlFromFileRule.getContent();
			byte[] xacmlPolicyByteArray = xacmlPolicyAttachment.getData();
		}

	}

}