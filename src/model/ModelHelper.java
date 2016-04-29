package model;

import java.io.File;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import main.Config;
import main.MatchingHelper;
import model.citeseer.Document;
import model.dblp.Dblp;
import model.merged.Paper;
import model.merged.Papers;

/**
 * @author Alina Maria Ciobanu
 */
public class ModelHelper 
{
	private static final String READ_ERROR_MESSAGE_FORMAT  = "File '%s' could not be read";
	private static final String WRITE_ERROR_MESSAGE_FORMAT = "File '%s' could not be created";
	
	/**
	 * reads CiteSeer record from an XML file
	 */
	public static Document readCiteseerRecord(String filePath, JAXBContext jaxbContext)
	{
		try
		{
			File file = new File(filePath);

			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			Document record = (Document) unmarshaller.unmarshal(file);

			return record;
		}
		catch (JAXBException ex)
		{
			System.out.println(String.format(READ_ERROR_MESSAGE_FORMAT, filePath));
			
			Throwable linkedEx = ex.getLinkedException();

			if (Config.VERBOSITY_LEVEL >= 2)
			{
				System.out.println(ex.getMessage());
			}
			if (Config.VERBOSITY_LEVEL == 3)
			{
				ex.printStackTrace();

				if (linkedEx != null)
				{
					linkedEx.printStackTrace();
				}
			}
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, String.format(READ_ERROR_MESSAGE_FORMAT, filePath));
		}

		return null;
	}
	
	/**
	 * writes merged paper (DBLP and CiteSeer) to an XML file
	 */
	public static void writeModel(Paper paper, String filePath)
	{
		try
		{
			JAXBContext jaxbContext = JAXBContext.newInstance(Paper.class);

			Marshaller marshaller = jaxbContext.createMarshaller();

			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, Charset.forName("UTF8").toString());

			marshaller.marshal(paper, new File(filePath));
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, String.format(WRITE_ERROR_MESSAGE_FORMAT, filePath));
		}
	}
	
	public static void writeModel(Document paper, String filePath)
	{
		try
		{
			JAXBContext jaxbContext = JAXBContext.newInstance(Document.class);

			Marshaller marshaller = jaxbContext.createMarshaller();

			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, Charset.forName("UTF8").toString());

			marshaller.marshal(paper, new File(filePath));
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, String.format(WRITE_ERROR_MESSAGE_FORMAT, filePath));
		}
	}
	
	/**
	 * writes merged papers (DBLP and CiteSeer) to an XML file
	 */
	public static void writeModel(Papers papers, String filePath)
	{
		try
		{
			JAXBContext jaxbContext = JAXBContext.newInstance(Papers.class);

			Marshaller marshaller = jaxbContext.createMarshaller();

			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, Charset.forName("UTF8").toString());

			marshaller.marshal(papers, new File(filePath));
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, String.format(WRITE_ERROR_MESSAGE_FORMAT, filePath));
		}
	}
	
	/**
	 * writes DBLP entries to an XML file
	 */
	public static void writeModel(Dblp dblp, String filePath)
	{
		try
		{
			JAXBContext jaxbContext = JAXBContext.newInstance(Dblp.class);

			Marshaller marshaller = jaxbContext.createMarshaller();

			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, Charset.forName("UTF8").toString());

			marshaller.marshal(dblp, new File(filePath));
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, String.format(WRITE_ERROR_MESSAGE_FORMAT, filePath));
		}
	}
}