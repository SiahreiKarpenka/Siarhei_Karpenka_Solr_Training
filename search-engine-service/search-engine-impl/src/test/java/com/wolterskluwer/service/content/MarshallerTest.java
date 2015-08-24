package com.wolterskluwer.service.content;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collections;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import com.wolterskluwer.service.content.validation.orchestration.Context;
import com.wolterskluwer.service.content.validation.orchestration.Context.Filters;
import com.wolterskluwer.service.content.validation.orchestration.Context.Filters.MimeType;
import com.wolterskluwer.service.content.validation.orchestration.Context.Filters.Path;
import com.wolterskluwer.service.content.validation.orchestration.Metadata;
import com.wolterskluwer.service.content.validation.orchestration.Orchestration;
import com.wolterskluwer.service.content.validation.orchestration.Param;
import com.wolterskluwer.service.content.validation.orchestration.Property;
import com.wolterskluwer.service.content.validation.orchestration.SimpleLiteral;
import com.wolterskluwer.service.content.validation.orchestration.Validation;
import com.wolterskluwer.service.content.validation.orchestration.XmlReporter;

public class MarshallerTest {

	//@Test
	public void marshall() throws FileNotFoundException, JAXBException {

		Orchestration orchestration = new Orchestration();
		orchestration.setHandleArchives(true);

		XmlReporter reporter = new XmlReporter();
		reporter.setId("repId1");

		Param reporterParam = new Param();
		reporterParam.setName("repParam1");
		reporterParam.setValue("repParam1Value");

		reporter.setParam(Collections.singletonList(reporterParam));

		orchestration.setReporter(Collections.singletonList(reporter));

		Metadata metadata = new Metadata();
		metadata.setCreator(getLiteral("creator"));
		metadata.setDate(getLiteral("date"));
		metadata.setDescription(getLiteral("descr"));
		orchestration.setMetadata(metadata);

		Property property = new Property();
		property.setName("propName1");
		property.setValue("propValue1");
		orchestration.getProperty().add(property);

		Context context = new Context();
		Filters filters = new Filters();
		MimeType mimeType = new MimeType();
		mimeType.setIgnoreParams(true);
		mimeType.setName("somemimename");
		filters.getMimeType().add(mimeType);
		Path path = new Path();
		path.setValue("somepathvalue");
		path.setCaseSensitive(true);
		filters.getPath().add(path);
		context.setFilters(filters);
		Validation validation = new Validation();
		validation.setBreakOnError(true);
		validation.setRefId("somevalidid");
		Param param = new Param();
		param.setName("paramname");
		param.setValue("param value");
		validation.getParam().add(param);
		context.getValidation().add(validation);
		orchestration.getContext().add(context);

		JAXBContext jaxbcontext = JAXBContext
				.newInstance("com.wolterskluwer.service.content.validation.orchestration");
	
		Marshaller marshaller = jaxbcontext.createMarshaller();
		marshaller.marshal(orchestration, new FileOutputStream(new File("d:\\Workspaces\\osa2workspace\\temp\\orchestration2.xml")));
	}

	//@Test
	public void unmarshall() throws JAXBException {

		JAXBContext context = JAXBContext
				.newInstance("com.wolterskluwer.service.content.validation.orchestration");
		Unmarshaller unmarshaller = context.createUnmarshaller();
		File file = new File(
				"d:\\Workspaces\\osa2workspace\\temp\\orchestration2.xml");
		JAXBElement<Orchestration> jaxbElement = unmarshaller.unmarshal(
				new StreamSource(file), Orchestration.class);
		Orchestration result = jaxbElement.getValue();
		System.out.println(result);
	}

	private SimpleLiteral getLiteral(String text) {
		SimpleLiteral simpleLiteral = new SimpleLiteral();
		simpleLiteral.getContent().add(text);
		return simpleLiteral;
	}
}
