/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kurscbrf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Fedorenko Aleksandr
 */
public class Request {

    public static String req() {
        return req(getLatestDateTime(), "USD");
    }

    public static String req(String valuta) {
        return req(getLatestDateTime(), valuta);
    }

    public static String req(String data, String valuta) {
        String answer = "";
        SOAPMessage soapMessage;
        SOAPPart part;
        SOAPEnvelope envelope;
        SOAPBody body;
        Name bodyName;
        SOAPElement bodyElement;
        String destination;
        SOAPConnectionFactory soapConnFactory;
        SOAPConnection connection;
        SOAPMessage reply;
        try {
            //Создаем сообщение
            soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();//Создаем сообщение. Сначала с помощью объекта MessageFactory создается собственно сообщение. В этот момент оно уже включает в себя разделы envelope и header, но они пока пусты                        
            part = soapMessage.getSOAPPart();//Создаем объекты, представляющие различные компоненты сообщения. Объект SOAPPart содержит             
            envelope = part.getEnvelope();//envelope, в который, в свою очередь,            
            body = envelope.getBody();//включено тело сообщения. Далее объявляются переменные, содержащие все необходимые ссылки, в частности, SOAPBody.            
            bodyName = envelope.createName("GetCursOnDateXML", null, "http://web.cbr.ru/");

            //Формирование тела сообщения
            //Создание главного элемента с учетом пространства имен            
            bodyElement = body.addChildElement(bodyName);
            bodyElement.addChildElement("On_date").addTextNode(data);

            soapMessage.saveChanges();
//            System.out.println("Запрос:");
//            XMLtoString(soapMessage);

            //Отправка сообщения и получение ответа
            //Установка адресата            
            destination = "http://www.cbr.ru/DailyInfoWebServ/DailyInfo.asmx";
            //Отправка

            soapConnFactory = SOAPConnectionFactory.newInstance();//Создаем соединение            
            connection = soapConnFactory.createConnection();
            reply = connection.call(soapMessage, destination);

            //ответ
            //Проверка полученного ответа
//            System.out.println("\nОтвет:");
            //writeXMLtoConsole(reply);
//            XMLtoString(reply);            
            connection.close();//Закрываем соединение
            answer = getKursFromXml(reply, valuta);

        } catch (SOAPException ex) {
            Logger.getLogger(Request.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }

    public static String getLatestDateTime() {
        String answer = "2099-09-07T18:13:00";
        SOAPMessage soapMessage;
        SOAPPart part;
        SOAPEnvelope envelope;
        SOAPBody body;
        Name bodyName;
        String destination;
        SOAPConnectionFactory soapConnFactory;
        SOAPConnection connection;
        SOAPMessage reply;
        try {
            //Создаем сообщение            
            soapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL).createMessage();//Создаем сообщение. Сначала с помощью объекта MessageFactory создается собственно сообщение. В этот момент оно уже включает в себя разделы envelope и header, но они пока пусты
            part = soapMessage.getSOAPPart();//Создаем объекты, представляющие различные компоненты сообщения. Объект SOAPPart содержит
            envelope = part.getEnvelope();//envelope, в который, в свою очередь,
            body = envelope.getBody();//включено тело сообщения. Далее объявляются переменные, содержащие все необходимые ссылки, в частности, SOAPBody.            
            bodyName = envelope.createName("GetLatestDateTime", null, "http://web.cbr.ru/");

            //Формирование тела сообщения
            //Создание главного элемента с учетом пространства имен            
            body.addChildElement(bodyName);
            soapMessage.saveChanges();
//            System.out.println("Запрос:");
//            XMLtoString(soapMessage);

            //Отправка сообщения и получение ответа
            //Установка адресата            
            destination = "http://www.cbr.ru/DailyInfoWebServ/DailyInfo.asmx";
            //Отправка

            soapConnFactory = SOAPConnectionFactory.newInstance();//Создаем соединение
            connection = soapConnFactory.createConnection();
            reply = connection.call(soapMessage, destination);

            //ответ
            //Проверка полученного ответа
//            System.out.println("\nОтвет:");
            //writeXMLtoConsole(reply);
//            XMLtoString(reply);
//            writeXMLtoSystemOut5(reply);
            connection.close();//Закрываем соединение
            answer = getLastDateFromXml(reply);
        } catch (UnsupportedOperationException | SOAPException ex) {
            Logger.getLogger(Request.class.getName()).log(Level.SEVERE, null, ex);
        }
        return answer;
    }

    private static String getKursFromXml(SOAPMessage soapMessage, String valuta) {
        String xPathString = "//ValuteCursOnDate/Vcurs"// Получить скомпилированный вариант XPath-выражения
                + "[starts-with(preceding-sibling::VchCode, '" + valuta + "')"
                + " or "
                + "starts-with(following-sibling::VchCode, '" + valuta + "')]";
        return findInXml(soapMessage, xPathString);
    }

    private static String getLastDateFromXml(SOAPMessage soapMessage) {
        String xPathString = "//GetLatestDateTimeResponse/GetLatestDateTimeResult"// Получить скомпилированный вариант XPath-выражения            
                ;
        return findInXml(soapMessage, xPathString);
    }

    // Печать цены книги у которой Title начинается с Yogasana
    // Варианты доступа к относительным узлам:
    // ancestor , ancestor-or-self, descendant, descendant-or-self
    // following, following-sibling, namespace, preceding, preceding-sibling
    private static String findInXml(SOAPMessage soapMessage, String xPathString) {
        try {
            //Создание XSLT-процессора
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer;
            transformer = transformerFactory.newTransformer();
            //Получение содержимого ответа
            Source sourceContent;
            sourceContent = soapMessage.getSOAPPart().getContent();
            //Задание выходного потока для результата преобразования
            File xmlFile = new File("info.xml");
            FileWriter fileWriter = new FileWriter(xmlFile);
            StreamResult result = new StreamResult(fileWriter);
            transformer.transform(sourceContent, result);
            // Создается построитель документа
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            // Создается дерево DOM документа из файла
            Document document = documentBuilder.parse(xmlFile);
//            System.out.println("Example 5 - Печать курса валюты у которой VchCode начинается с " + valuta);
            XPathFactory pathFactory = XPathFactory.newInstance();// Создать XPathFactory
            XPath xpath = pathFactory.newXPath();// Создать XPath
            XPathExpression expr = xpath.compile(xPathString);
            NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);// Применить XPath-выражение к документу для поиска нужных элементов
//            System.out.println("nodes.getLength:" + nodes.getLength());

            if (nodes.getLength() == 1) {
                Node n = nodes.item(0);
                return n.getTextContent();
            }

//            System.out.println("Value:" + answer);
//            System.out.println();
        } catch (IOException | ParserConfigurationException | SOAPException | TransformerException | XPathExpressionException | DOMException | SAXException ex) {
            Logger.getLogger(Request.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
    private static void XMLtoString(SOAPMessage soapMessage) {
        try {
            //Создание XSLT-процессора
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            //Получение содержимого ответа
            Source sourceContent = soapMessage.getSOAPPart().getContent();
            //Задание выходного потока для результата преобразования
            File xmlFile = new File("info.xml");
            FileWriter fileWriter = new FileWriter(xmlFile);
            StreamResult result = new StreamResult(fileWriter);
            transformer.transform(sourceContent, result);
            // Let's get XML file as String using BufferedReader 
            // FileReader uses platform's default character encoding 
            // if you need to specify a different encoding, use InputStreamReader 
            Reader fileReader = new FileReader(xmlFile);
            try (BufferedReader bufReader = new BufferedReader(fileReader)) {
                StringBuilder sb = new StringBuilder();
                String line = bufReader.readLine();
                while (line != null) {
                    sb.append(line).append("\n");
                    line = bufReader.readLine();
                }
                String xml2String = sb.toString();
                String[] xmlArray = xml2String.split(">");

                for (String str : xmlArray) {
                    System.out.println(str + ">");
                }
//                System.out.println("XML to String using BufferedReader : ");
//                System.out.println(xml2String);
            }
        } catch (TransformerConfigurationException ex) {
//            Logger.getLogger(KursCBRF.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException | SOAPException ex) {
//            Logger.getLogger(KursCBRF.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Request.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
