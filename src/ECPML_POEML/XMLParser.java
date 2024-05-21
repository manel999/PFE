package ECPML_POEML;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class XMLParser {

    public static void main(String[] args) {
        try {
            // Map containing tag replacements
           
            // Define the base directory containing folders with XML files
            String baseDir = "C:\\Users\\vappyq\\Documents\\ProjetM2\\ECPML\\modeles-in-XML";
            File logFile = new File(baseDir, "validation_errors.txt");
            verifPOEML2.openLog(logFile);
            // Walk through each file in each directory under baseDir
            Files.walk(Paths.get(baseDir))
                 .filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".xml"))  // Ensure only XML files are processed
                 .filter(path -> !path.getFileName().toString().equalsIgnoreCase("output.xml"))
                 .forEach(path -> {
                     System.out.println("Processing file: " + path.getFileName()); // Print the file name
                     processFile(path.toFile()); // Process the file
                 });
            
            verifPOEML2.closeLog();
            //System.out.println("XML file" + outputFile+"verified");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Element findTaskByName(Document document, String taskName) {
        NodeList taskNodes = document.getElementsByTagName("Task");
        for (int i = 0; i < taskNodes.getLength(); i++) {
            Node taskNode = taskNodes.item(i);
            if (taskNode.getNodeType() == Node.ELEMENT_NODE) {
                Element taskElement = (Element) taskNode;
                String nameAttribute = taskElement.getAttribute("name");
                if (nameAttribute.equals(taskName)) {
                    return taskElement;
                }
            }
        }
        return null;
    }
    
    private static void removeUnusedLinkTags(Document document) {
        // Define the tags to be removed
        String[] tagsToRemove = {"linkToSuccessors", "linkToPredecessors"};

        for (String tag : tagsToRemove) {
            NodeList elements = document.getElementsByTagName(tag);
            // Process each element found
            while (elements.getLength() > 0) {
                Node node = elements.item(0);
                Node parent = node.getParentNode();

                // Move all children of the current node to be children of the parent node, just before the current node
                while (node.hasChildNodes()) {
                    parent.insertBefore(node.getFirstChild(), node);
                }

                // Once all children are moved, remove the now-empty node
                parent.removeChild(node);
            }
        }
    }

    
 

    
    
    private static void wrapTaskPrecedences(Document document) {
        // Get all Task elements in the document
        NodeList tasks = document.getElementsByTagName("Task");
        for (int i = 0; i < tasks.getLength(); i++) {
            Element task = (Element) tasks.item(i);
            // Apply the wrapping function to each Task element
            wrapPrecedencesForNode(task);
        }
    }

    private static void wrapPrecedencesForNode(Element taskElement) {
        // Check if this Task element already has a TaskPrecedences container
        NodeList existingPrecedences = taskElement.getElementsByTagName("TaskPrecedences");
        if (existingPrecedences.getLength() > 0) {
            // If already exists, we remove it to recreate it anew (to avoid duplicates or misplaced elements)
            taskElement.removeChild(existingPrecedences.item(0));
        }

        // Create a new TaskPrecedences element
        Document doc = taskElement.getOwnerDocument();
        Element taskPrecedencesElement = doc.createElement("TaskPrecedences");

        // Get all TaskPrecedence elements under this Task
        NodeList taskPrecedences = taskElement.getElementsByTagName("TaskPrecedence");
        List<Element> precedences = new ArrayList<>();

        for (int i = 0; i < taskPrecedences.getLength(); i++) {
            Element tp = (Element) taskPrecedences.item(i);
            if (tp.getParentNode() == taskElement) { // Ensure it's a direct child to avoid nested handling conflicts
                precedences.add(tp);
            }
        }

        // Move each TaskPrecedence under the TaskPrecedences container
        for (Element tp : precedences) {
        	
                if (tp.hasAttribute("linkKind")) {
                    String kindValue = tp.getAttribute("linkKind");
                    tp.removeAttribute("linkKind");
                    tp.setAttribute("kind", kindValue);
                }
            taskPrecedencesElement.appendChild(tp);
        }

        // Add the new TaskPrecedences container to the Task element if it contains any TaskPrecedence
        if (taskPrecedencesElement.hasChildNodes()) {
            taskElement.appendChild(taskPrecedencesElement);
        }
    }



    
    
    

    public static boolean isCollaborativeTask(Element taskElement) {
        // Check for null to avoid NullPointerException
        if (taskElement == null) {
            return false;
        }

        // Get all elements by tag name 'taskPerformers'
        NodeList performersList = taskElement.getElementsByTagName("taskPerformers");
        
        // If there's at least one 'taskPerformers' tag, it's a CollaborativeTask
        return performersList.getLength() > 0;
    }
    
    
    private static void replaceProductTagsWithImpactingElements(Document doc) {
        NodeList productList = doc.getElementsByTagName("Product");

        for (int i = 0; i < productList.getLength(); i++) {
            Element productElement = (Element) productList.item(i);
            String productName = productElement.getAttribute("name");

            NodeList productImpactList = productElement.getElementsByTagName("ProductImpact");
            for (int j = 0; j < productImpactList.getLength(); j++) {
                Element productImpactElement = (Element) productImpactList.item(j);

                // Collect all <product> tags first to avoid modifying NodeList directly
                NodeList productTags = productImpactElement.getElementsByTagName("product");
                List<Node> productsToModify = new ArrayList<>();
                for (int k = 0; k < productTags.getLength(); k++) {
                    productsToModify.add(productTags.item(k));
                }

                for (Node productTag : productsToModify) {
                    if (productTag.getNodeType() == Node.ELEMENT_NODE) {
                        // Create a new 'ImpactedElement' to replace 'product'
                        Element impactedElement = doc.createElement("ImpactedElement");

                        // Copy all attributes from 'product' to 'ImpactedElement'
                        NamedNodeMap attributes = productTag.getAttributes();
                        for (int attrIndex = 0; attrIndex < attributes.getLength(); attrIndex++) {
                            impactedElement.setAttributeNode((org.w3c.dom.Attr) attributes.item(attrIndex).cloneNode(true));
                        }

                        // Transfer all child nodes
                        while (productTag.hasChildNodes()) {
                            impactedElement.appendChild(productTag.getFirstChild());
                        }

                        // Replace the old 'product' node with 'ImpactedElement'
                        productImpactElement.replaceChild(impactedElement, productTag);
                    }
                }

                // After processing all products, add 'ImpactingElement' tag within 'ProductImpact'
                Element impactingElement = doc.createElement("ImpactingElement");
                impactingElement.setAttribute("ref", productName);
                productImpactElement.appendChild(impactingElement);
            }
        }
    }
    
    
    private static void replaceProductTagsWithnestedElements(Document doc) {
        NodeList productList = doc.getElementsByTagName("Product");

        for (int i = 0; i < productList.getLength(); i++) {
            Element productElement = (Element) productList.item(i);
            String productName = productElement.getAttribute("name");

            NodeList productImpactList = productElement.getElementsByTagName("Aggregation");
            for (int j = 0; j < productImpactList.getLength(); j++) {
                Element productImpactElement = (Element) productImpactList.item(j);

                // Collect all <product> tags first to avoid modifying NodeList directly
                NodeList productTags = productImpactElement.getElementsByTagName("product");
                List<Node> productsToModify = new ArrayList<>();
                for (int k = 0; k < productTags.getLength(); k++) {
                    productsToModify.add(productTags.item(k));
                }

                for (Node productTag : productsToModify) {
                    if (productTag.getNodeType() == Node.ELEMENT_NODE) {
                        // Create a new 'ImpactedElement' to replace 'product'
                        Element impactedElement = doc.createElement("component");

                        // Copy all attributes from 'product' to 'ImpactedElement'
                        NamedNodeMap attributes = productTag.getAttributes();
                        for (int attrIndex = 0; attrIndex < attributes.getLength(); attrIndex++) {
                            impactedElement.setAttributeNode((org.w3c.dom.Attr) attributes.item(attrIndex).cloneNode(true));
                        }

                        // Transfer all child nodes
                        while (productTag.hasChildNodes()) {
                            impactedElement.appendChild(productTag.getFirstChild());
                        }

                        // Replace the old 'product' node with 'ImpactedElement'
                        productImpactElement.replaceChild(impactedElement, productTag);
                    }
                }

                // After processing all products, add 'ImpactingElement' tag within 'ProductImpact'
                Element impactingElement = doc.createElement("aggregate");
                impactingElement.setAttribute("ref", productName);
                productImpactElement.appendChild(impactingElement);
            }
        }
    }







    private static void processFile(File inputFile) {
        try {
        	
        	 Map<String, String> tagReplacements = new HashMap<>();
             // Définition des remplacements de tags à effectuer lors de la transformation XML
                tagReplacements.put("taskParameters", "TaskParameters");
                tagReplacements.put("taskParameter", "TaskParameter");
                //tagReplacements.put("direction", "direction");
                tagReplacements.put("linkedTask", "LinkedTask");
                //tagReplacements.put("product", "product");
                tagReplacements.put("taskPerformer", "TaskPerformance");
                tagReplacements.put("Role", "Role");
                //tagReplacements.put("linkKind", "WorkSequenceKind");
                tagReplacements.put("successor", "successor");
                tagReplacements.put("predecessor", "predecessor");
                tagReplacements.put("linkToPredecessor", "TaskPrecedence");
                tagReplacements.put("linkToSuccessor", "TaskPrecedence");
                //tagReplacements.put("LinkTosuccessors", "TaskPrecedences");
                //tagReplacements.put("LinkToPredecessors", "TaskPrecedences");
                tagReplacements.put("WorkProduct", "Product");
                tagReplacements.put("nestedProduct", "Aggregation");
                tagReplacements.put("nestedProducts", "Aggregations");
                tagReplacements.put("impactedProduct", "ProductImpact");
                tagReplacements.put("WorkProducts", "Products");
                //tagReplacements.put("linkToSuccessors", "WorkSequences");
                //tagReplacements.put("linkToPredecessors", "WorkSequences");
                tagReplacements.put("taskPerformers", "TaskPerformances");
                tagReplacements.put("impactedProducts", "ProductsImpact");
                tagReplacements.put("Tasks", "Tasks");
                tagReplacements.put("Task", "Task");
                tagReplacements.put("ECPMLModel", "POEMLModel");

            File outputFile = new File(inputFile.getParent(), "output.xml");  // Save the output file in the same directory

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputFile);

            //Map<String, String> tagReplacements = initializeTagReplacements();
            performTagReplacements(document.getDocumentElement(), tagReplacements);
            removeUnusedLinkTags(document);
            wrapTaskPrecedences(document);
            replaceProductTagsWithImpactingElements(document);
            replaceProductTagsWithnestedElements(document);
            
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(outputFile));

            System.out.println("Processed file saved to " + outputFile.getPath());
            
            verifPOEML2.validateXML(outputFile);
            
        } catch (Exception e) {
            System.err.println("Error processing file: " + inputFile.getPath());
            e.printStackTrace();
        }
    }
    
    
    

    

    private static void performTagReplacements(Element element, Map<String, String> tagReplacements) {
        String tagName = element.getTagName();
        
        if (!tagName.equals("toolDefinition") && tagReplacements.containsKey(tagName)) {
            Document document = element.getOwnerDocument();
            Element newElement = document.createElement(tagReplacements.get(tagName));
            
            if (tagName.equals("taskPerformer")) {
                newElement.setAttribute("Type", "Performer");
            
                NodeList children = element.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child instanceof Element) {
                        Element childElement = (Element) child;
                        String childTagName = childElement.getTagName();
                        if (childTagName.equals("linkedTask")) {
                            Element translatedChild = document.createElement("performedTask"); // New tag for POEML
                            translatedChild.setAttribute("ref", childElement.getAttribute("ref"));
                            newElement.appendChild(translatedChild);
                        } else if (childTagName.equals("Role")) {
                            // Removed duplicate import of the original "Role" element
                            Element translatedChild = document.createElement("performer"); // New tag for POEML
                            translatedChild.setAttribute("ref", childElement.getAttribute("ref"));
                            newElement.appendChild(translatedChild);
                        } 
                    }
                }

            
            
            // Replace the original taskPerformer with the new Performer element
            //element.getParentNode().replaceChild(newElement, element);
            }
            
            
                
            
             
            NodeList useNodes = element.getElementsByTagName("use");
            for (int i = 0; i < useNodes.getLength(); i++) {
                Node useNode = useNodes.item(i);
                if (useNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element useElement = (Element) useNode;
                    String toolRef = useElement.getElementsByTagName("tool").item(0).getAttributes().getNamedItem("ref").getNodeValue();
                    String referencedTaskName = useElement.getElementsByTagName("managedTask").item(0).getAttributes().getNamedItem("ref").getNodeValue();
                    Element referencedTaskElement = findTaskByName(document, referencedTaskName);
                    if (referencedTaskElement != null) {
                        referencedTaskElement.setAttribute("Description", toolRef);
                    }
                }
            }
            
            if (tagName.equals("Task") && isCollaborativeTask(element)) {
            	NamedNodeMap attributes1 = element.getAttributes();
            	for (int i = 0; i < attributes1.getLength(); i++) {
            	    Attr attr = (Attr) attributes1.item(i);
            	    // Exclude the 'type' attribute from copying
            	    if (!attr.getName().equals("type")) {
            	        newElement.setAttribute(attr.getName(), attr.getValue());
            	    }
            	}
                NodeList taskPerformerNodes = element.getElementsByTagName("taskPerformer");
                int numberOfTaskPerformers = taskPerformerNodes.getLength();
                System.out.println(numberOfTaskPerformers);
                Element sousTasksElement = document.createElement("sous-tasks");
                for (int i = 0; i < taskPerformerNodes.getLength(); i++) {
                    Node taskPerformerNode = taskPerformerNodes.item(i);
                    if (taskPerformerNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element aggregationElement = document.createElement("sous-task");
                        Element taskElement = document.createElement("Task");
                        taskElement.setAttribute("name", element.getAttribute("name") + " instance " + (i + 1));
                        
                        // Add description if it exists in the original task
                        if (element.hasAttribute("Description")) {
                            taskElement.setAttribute("Description", element.getAttribute("Description"));
                        }

                        // Copy attributes excluding 'type'
                        NamedNodeMap attributes = element.getAttributes();
                        for (int j = 0; j < attributes.getLength(); j++) {
                            Attr attr = (Attr) attributes.item(j);
                            if (!attr.getName().equals("type")&& !attr.getName().equals("name")) {
                                taskElement.setAttribute(attr.getName(), attr.getValue());
                            }
                        }

                        // Copy child nodes except 'taskPerformers'
                        NodeList childNodes = element.getChildNodes();
                        for (int j = 0; j < childNodes.getLength(); j++) {
                            Node childNode = childNodes.item(j);
                            if (!(childNode instanceof Element) || !((Element) childNode).getTagName().equals("taskPerformers")) {
                                Node importedNode = document.importNode(childNode, true);
                                taskElement.appendChild(importedNode);
                            }
                        }

                        // Append performers
                        Element performerElement = document.createElement("taskPerformer");
                        NodeList performerChildren = taskPerformerNode.getChildNodes();
                        for (int k = 0; k < performerChildren.getLength(); k++) {
                            Node performerChild = performerChildren.item(k);
                            if (performerChild instanceof Element) {
                                Node importedNode = document.importNode(performerChild, true);
                                performerElement.appendChild(importedNode);
                            }
                        }
                        
                        taskElement.appendChild(performerElement);
                        aggregationElement.appendChild(taskElement);
                        sousTasksElement.appendChild(aggregationElement);
                    }
                }
                newElement.appendChild(sousTasksElement);
            } else {
                NamedNodeMap attributes = element.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Attr attr = (Attr) attributes.item(i);
                    if (!tagName.equals("Task") || !attr.getName().equals("type")) {
                        newElement.setAttribute(attr.getName(), attr.getValue());
                    }
                    //newElement.setAttribute(attr.getName(), attr.getValue());
                }
             // Copier les enfants de l'ancien élément vers le nouveau
                NodeList childNodes = element.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node childNode = childNodes.item(i);
                    String childTagName = (childNode instanceof Element) ? ((Element) childNode).getTagName() : null;
                    
                    if (!(childTagName != null && (( childTagName.equals("Role") && element.getTagName().equals("taskPerformer")) ||(childTagName.equals("linkedTask") && element.getTagName().equals("taskPerformer")) || (childTagName.equals("toolDefinition") || childTagName.equals("toolsDefinition") || childTagName.equals("use") || childTagName.equals("uses")) ))) {
                        Node importedNode = document.importNode(childNode, true);
                        newElement.appendChild(importedNode);
                    }
                }
            }
            
         // Remplacer l'ancien élément par le nouveau dans le document
            element.getParentNode().replaceChild(newElement, element);
            element = newElement;
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
            	
                performTagReplacements((Element) child, tagReplacements);
            }
        }
    }

}
