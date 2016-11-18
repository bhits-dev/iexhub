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
package org.iexhub.services;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import jersey.repackaged.com.google.common.base.Equivalence;
import jersey.repackaged.com.google.common.base.Predicate;
import jersey.repackaged.com.google.common.collect.Collections2;
import jersey.repackaged.com.google.common.collect.MapDifference;
import jersey.repackaged.com.google.common.collect.Maps;

import org.iexhub.exceptions.InvalidExchangeFormatException;
import org.iexhub.exceptions.InvalidSourceModelException;
import org.iexhub.exceptions.InvalidTargetModelException;
import org.iexhub.exceptions.MessageTransformException;
import org.iexhub.exceptions.SourceMapMissingException;
import org.iexhub.exceptions.SourceMsgMissingException;
import org.iexhub.exceptions.TargetMapMissingException;
import org.iexhub.exceptions.UnexpectedServerException;
import org.openhealthtools.mdht.mdmi.Mdmi;
import org.openhealthtools.mdht.mdmi.MdmiConfig;
import org.openhealthtools.mdht.mdmi.MdmiMessage;
import org.openhealthtools.mdht.mdmi.MdmiModelRef;
import org.openhealthtools.mdht.mdmi.MdmiTransferInfo;
import org.openhealthtools.mdht.mdmi.model.MdmiBusinessElementReference;

/**
 * @author A. Sute
 *
 */
@Deprecated
@Path("/TranslateOutbound")
public class TranslateOutboundService
{
	@GET
	@Produces("application/xml")
	public Response translateOutbound(@QueryParam("applicationId") String applicationId,
			@QueryParam("exchangeFormat") String exchangeFormat,
			@QueryParam("sourceDataSet") String sourceDataSet)
	{
		String srcMap = "test/EhrMap.xmi";
		String srcMdl = "EHR1.ModelName";
		String trgMdl = null;
		String trgMsg = null;
		String cnvElm = "";
		File sourceDataSetFile = null;
		
		File rootDir = new File(System.getProperties().getProperty("user.dir"));

		// initialize the runtime, using the current folder as the root folder
		Mdmi.INSTANCE.initialize(rootDir);
		Mdmi.INSTANCE.start();

		String retVal = null;
		try
		{
			String trgMap = null;
			if (exchangeFormat.compareToIgnoreCase("ccda") == 0)
			{
				trgMap = "test/CCDAMap.9.1.xmi";
				trgMdl = "CCDMessageGroup.CCD";
				trgMsg = "test/target_minimal.xml";
			}
			else
			if (exchangeFormat.compareToIgnoreCase("fhir") == 0)
			{
				trgMap = "test/FhirPatient.xmi";
				trgMdl = "FHIRLocalTestMap.FHIRTest99";
				trgMsg = "test/PatientTarget.xml";
			}
			else
			{
				throw new InvalidExchangeFormatException("Error - invalid exchange format specified.  'CCDA' or 'FHIR' supported.  '" + srcMap + "' does not exist");
			}

			// 1. check to make sure the maps and messages exist
			File f = Mdmi.INSTANCE.fileFromRelPath(srcMap);
			if (!f.exists() || !f.isFile())
			{
				throw new SourceMapMissingException("Error - source map file '" + srcMap + "' does not exist");
			}
			else
			{
				f = Mdmi.INSTANCE.fileFromRelPath(trgMap);
				if (!f.exists() || !f.isFile())
				{
					throw new TargetMapMissingException("Error - target map file '" + trgMap + "' does not exist");
				}
				else
				{
					sourceDataSetFile = Mdmi.INSTANCE.fileFromRelPath(sourceDataSet);
					if (!sourceDataSetFile.exists() || !sourceDataSetFile.isFile())
					{
						// The source message may be a URI - verify if that's the case...
						sourceDataSetFile = new File(sourceDataSet);
					}
					
					if (!sourceDataSetFile.exists() || !sourceDataSetFile.isFile())
					{
						throw new SourceMsgMissingException("Error - source message file '" + sourceDataSet + "' does not exist");
					}
					else
					{
						f = Mdmi.INSTANCE.fileFromRelPath(trgMsg);
						if (!f.exists() || !f.isFile())
						{
							throw new TargetMapMissingException("Error - target message file '" + trgMsg + "' does not exist");
						}
						else
						{
							// 2. make sure the qualified message names are spelled right
							String[] a = srcMdl.split("\\.");
							if (a == null || a.length != 2)
							{
								throw new InvalidSourceModelException("Error - invalid source model '" + srcMdl + "', must be formatted as MapName.MessageName");
							}
							else
							{
								String srcMapName = a[0];
								String srcMsgMdl = a[1];
					
								a = trgMdl.split("\\.");
								if (a == null || a.length != 2)
								{
									throw new InvalidTargetModelException("Error - invalid target model '" + trgMdl + "', must be formatted as MapName.MessageName");
								}
								else
								{
									try
									{
										String trgMapName = a[0];
										String trgMsgMdl = a[1];
							
										// 3. parse the elements array
										final ArrayList<String> elements = new ArrayList<String>();
										String[] ss = cnvElm.split(";");
										for (String s : ss) {
											if (s != null && s.trim().length() > 0) {
												elements.add(s);
											}
										}
							
										// 4. load the maps into the runtime.
										Mdmi.INSTANCE.getConfig().putMapInfo(new MdmiConfig.MapInfo(srcMapName, srcMap));
										Mdmi.INSTANCE.getConfig().putMapInfo(new MdmiConfig.MapInfo(trgMapName, trgMap));
										Mdmi.INSTANCE.getResolver().resolveConfig(Mdmi.INSTANCE.getConfig());
							
										// 5. Construct the parameters to the call based on the values passed in
										MdmiModelRef sMod = new MdmiModelRef(srcMapName, srcMsgMdl);
										MdmiMessage sMsg = new MdmiMessage(sourceDataSetFile);
										MdmiModelRef tMod = new MdmiModelRef(trgMapName, trgMsgMdl);
										MdmiMessage tMsg = new MdmiMessage(Mdmi.INSTANCE.fileFromRelPath(trgMsg));
							
										Map<String, MdmiBusinessElementReference> left = sMod.getModel().getBusinessElementHashMap();
							
										Map<String, MdmiBusinessElementReference> right = tMod.getModel().getBusinessElementHashMap();
							
										Equivalence<MdmiBusinessElementReference> valueEquivalence = new Equivalence<MdmiBusinessElementReference>() {
											@Override
											protected boolean doEquivalent(MdmiBusinessElementReference a, MdmiBusinessElementReference b) {
												return a.getUniqueIdentifier().equals(b.getUniqueIdentifier());
											}
							
											@Override
											protected int doHash(MdmiBusinessElementReference t) {
												return t.getUniqueIdentifier().hashCode();
											}
										};
							
										MapDifference<String, MdmiBusinessElementReference> differences = Maps.difference(
											left, right, valueEquivalence);
							
										Predicate<MdmiBusinessElementReference> predicate = new Predicate<MdmiBusinessElementReference>() {
											@Override
											public boolean apply(MdmiBusinessElementReference input) {
							
												if (!elements.isEmpty()) {
													for (String element : elements) {
														if (input.getName().equalsIgnoreCase(element)) {
															return true;
														}
							
													}
													return false;
												}
												return true;
							
											}
										};
										;
							
										ArrayList<MdmiBusinessElementReference> bers = new ArrayList<MdmiBusinessElementReference>();
										bers.addAll(Collections2.filter(differences.entriesInCommon().values(), predicate));
							
										MdmiTransferInfo ti = new MdmiTransferInfo(sMod, sMsg, tMod, tMsg, bers);
										ti.useDictionary = true;
							
										// 6. call the runtime
										Mdmi.INSTANCE.executeTransfer(ti);
							
										// 7. set the return value
										retVal = tMsg.getDataAsString();
									}
									catch (Exception e)
									{
										throw new MessageTransformException(e.getMessage());
									}
								}
							}
						}
					}
				}
			}
		}
		catch (InvalidSourceModelException |
			   InvalidTargetModelException |
			   MessageTransformException |
			   SourceMapMissingException |
			   SourceMsgMissingException |
			   TargetMapMissingException
			   ex)
		{
			throw ex;
		}
		catch (Exception ex)
		{
			throw new UnexpectedServerException("Error - " + ex.getMessage());
		}
		finally
		{
			Mdmi.INSTANCE.stop();			
		}
		
		return Response.status(Response.Status.OK).entity(retVal).type(MediaType.APPLICATION_XML).build();
	}
	
	public static void saveResults(String content) throws Exception
	{
		FileOutputStream fop = null;
		File file;

		file = new File("resultsOutbound.xml");
		fop = new FileOutputStream(file);

		// if file doesn't exist, then create it
		if (!file.exists()) {
			file.createNewFile();
		}

		// get the content in bytes
		byte[] contentInBytes = content.getBytes();

		fop.write(contentInBytes);
		fop.flush();
		fop.close();
	}
}