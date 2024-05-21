package ECPML_POEML;

import java.io.*;
import java.lang.module.ModuleDescriptor.Builder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class verif_ECPML {
	
	public static void validateTags(Document doc) throws Exception {
        // Define the set of allowed tags (array2)
        List<String> allowedTags = List.of("taskParameters", "taskParameter", "linkedTask", "WorkProduct", "linkToSuccessors",
            "taskPerformer", "taskPerformers", "Role", "Roles", "successor", "predecessor", "linkToPredecessors",
            "linkToPredecessor", "linkToSuccessor", "linkToPredecessor", "ImpactedProduct", "product", "ImpactedProducts",
            "Tasks", "Task", "ECPMLModel", "performer","toolsDefinition","toolDefinition","uses","use","managedTask","tool"
            , "WorkProduct", "nestedProducts", "nestedProduct", "impactedProducts", "impactedProduct", "WorkProducts");

        // Create a list to store all tags found in the document
        List<String> foundTags = new ArrayList<>();

        // Collect all tags from the document
        NodeList allElements = doc.getElementsByTagName("*");
        for (int i = 0; i < allElements.getLength(); i++) {
            Element element = (Element) allElements.item(i);
            foundTags.add(element.getTagName());
        }

        // Check each found tag if it is allowed
        List<String> unknownTags = new ArrayList<>();
        for (String tag : foundTags) {
            if (!allowedTags.contains(tag)) {
                unknownTags.add(tag);
            }
        }

        if (!unknownTags.isEmpty()) {
            throw new Exception("Unknown tags found: " + unknownTags);
        }
    }

	private static void checkTasks(NodeList tasks) throws Exception {
        for (int i = 0; i < tasks.getLength(); i++) {
            Element task = (Element) tasks.item(i);
            NodeList Performers=task.getElementsByTagName("taskPerformer");
            if (!task.hasAttribute("name")) {
                throw new Exception("Task element number:"+i+" missing 'name' attribute.");
            }
            
            if (!(Performers.getLength()>=1)) {
                throw new Exception("No 'TaskPerformer' element found in Task :"+task.getAttribute("name"));
            }
        }
    }
	
	
	 private static void checkRoles(Document doc) throws Exception {
	        // Find the 'roles' parent element first
	        NodeList rolesList = doc.getElementsByTagName("Roles");
	        if (rolesList.getLength() == 0) {
	            throw new Exception("No 'Roles' element found in the document.");
	        }
	        for (int i = 0; i < rolesList.getLength(); i++) {
	        Node roleNode = rolesList.item(i);
	        if (roleNode.getNodeType() == Node.ELEMENT_NODE) {
	            Element roleElement = (Element) roleNode;
	            NodeList roleElements = roleElement.getElementsByTagName("Role");
	            for (int j = 0; j < roleElements.getLength(); j++) {
	                Element role = (Element) roleElements.item(j);
	                if (!role.hasAttribute("name")) {
	                    throw new Exception("Role tag missing 'name' attribute within 'Roles'.");
	                }
	            }
	        }
	        }
	     }
	 
	 private static boolean checkForNonEmptyProduct(NodeList nodeList) {
	        for (int i = 0; i < nodeList.getLength(); i++) {
	            NodeList products = ((Element) nodeList.item(i)).getElementsByTagName("product");
	            for (int j = 0; j < products.getLength(); j++) {
	                Element product = (Element) products.item(j);
	                if (product.hasAttributes()) {
	                    return true; // Found a non-empty product by attribute
	                }
	            }
	        }
	        return false;
	    }
	 
	 private static void checkProducts(Document doc) throws Exception {
	        NodeList products = doc.getElementsByTagName("workProduct");
	        for (int i = 0; i < products.getLength(); i++) {
	            Element product = (Element) products.item(i);
	            if (!product.hasAttribute("name")) {
	                throw new Exception("WorkProduct tag missing 'name' attribute.");
	            }
	            boolean isComposite = Boolean.parseBoolean(product.getAttribute("isComposite"));
	            
	            // Check for non-empty product tags by attributes, not just any child node
	            NodeList aggregations = product.getElementsByTagName("nestedProduct");
	            NodeList impactedProducts = product.getElementsByTagName("ImpactedProduct");

	            boolean hasNonEmptyProduct = checkForNonEmptyProduct(aggregations) || checkForNonEmptyProduct(impactedProducts);

	            if ((isComposite && !hasNonEmptyProduct)) {
	                throw new Exception("Product composition doesn't exist even though 'isComposite' is true attribute");
	            }else if((!isComposite && hasNonEmptyProduct)){
	                throw new Exception("Product composition exists even though 'isComposite' is false attribute for product named '" + product.getAttribute("name") + "'.");
	            }
	            
	            
	            
	        }
	    }
	 
	 
	 private static void checkTools(Document doc) throws Exception {
	        // Find the 'roles' parent element first
	        NodeList rolesList = doc.getElementsByTagName("toolsDefinitions");
	        for (int i = 0; i < rolesList.getLength(); i++) {
	        Node roleNode = rolesList.item(i);
	        if (roleNode.getNodeType() == Node.ELEMENT_NODE) {
	            Element roleElement = (Element) roleNode;
	            NodeList roleElements = roleElement.getElementsByTagName("toolDefinition");
	            for (int j = 0; j < roleElements.getLength(); j++) {
	                Element role = (Element) roleElements.item(j);
	                if (!role.hasAttribute("name")) {
	                    throw new Exception("toolDefinition tag missing 'name' attribute within 'toolDefinitions'.");
	                }
	            }
	        }
	        }
	     }
	 
	 private static void checkuses(Document doc) throws Exception {
	        NodeList taskPerformers = doc.getElementsByTagName("use");
	        
	        for (int i = 0; i < taskPerformers.getLength(); i++) {
	            Element taskPerformer = (Element) taskPerformers.item(i);
	            NodeList managedTasks = taskPerformer.getElementsByTagName("managedTask");
	            NodeList tools = taskPerformer.getElementsByTagName("tool");
	   
	          
	            if (managedTasks.getLength() != 1) {
	                throw new Exception("There must be exactly one 'managedTask' tag in use at index: " + (i + 1) + ", found: " + managedTasks.getLength());
	            }
	            if (tools.getLength() != 1) {
	                throw new Exception("There must be exactly one 'tool' tag in use at index: " + (i + 1) + ", found: " + tools.getLength());
	            }
	        }
	    }
	 
	 private static void checkTaskPerformers(Document doc) throws Exception {
	        NodeList taskPerformers = doc.getElementsByTagName("TaskPerformer");
	        
	        for (int i = 0; i < taskPerformers.getLength(); i++) {
	            Element taskPerformer = (Element) taskPerformers.item(i);
	            NodeList roles = taskPerformer.getElementsByTagName("role");
	            NodeList linkedTasks = taskPerformer.getElementsByTagName("linkedTask");
	            

	            if (roles.getLength() != 1) {
	                throw new Exception("There must be exactly one 'linkedTask' tag in TaskPerformance at index: " + (i + 1) + ", found: " + roles.getLength());
	            }
	            if (linkedTasks.getLength() != 1) {
	                throw new Exception("There must be exactly one 'role' tag in TaskPerformance at index: " + (i + 1) + ", found: " + linkedTasks.getLength());
	            }
	        }
	    }
	 
	 private static void checkTaskParameter(Document doc) throws Exception {
	        NodeList taskParameters = doc.getElementsByTagName("TaskParameter");
	        
	        for (int i = 0; i < taskParameters.getLength(); i++) {
	            Element taskParameter = (Element) taskParameters.item(i);
	            
	            // Check for the attribute direction
	            if (!taskParameter.hasAttribute("direction")) {
	                throw new Exception("TaskParameter element number: " + i + " missing 'direction' attribute.");
	            }

	            // Retrieve the direction attribute and validate its value
	            String direction = taskParameter.getAttribute("direction");
	            if (!(direction.equals("in") || direction.equals("out") || direction.equals("inout"))) {
	                throw new Exception("Invalid direction value: '" + direction + "' in TaskParameter at index: " + i);
	            }
	            
	            // Check for exactly one 'LinkedTask' tag
	            NodeList linkedTasks = taskParameter.getElementsByTagName("linkedTask");
	            if (linkedTasks.getLength() != 1) {
	                throw new Exception("There must be exactly one 'LinkedTask' tag in TaskParameter at index: " + i + ", found: " + linkedTasks.getLength());
	            }

	            // Check for exactly one 'product' tag
	            NodeList products = taskParameter.getElementsByTagName("product");
	            if (products.getLength() != 1) {
	                throw new Exception("There must be exactly one 'product' tag in TaskParameter at index: " + i + ", found: " + products.getLength());
	            }
	        }
	    }
	 
	 public static List<Element> getMultipleTagElements(Document doc, String[] tagNames) {
	        List<Element> elements = new ArrayList<>();
	        for (String tagName : tagNames) {
	            NodeList nodes = doc.getElementsByTagName(tagName);
	            for (int i = 0; i < nodes.getLength(); i++) {
	                elements.add((Element) nodes.item(i));
	            }
	        }
	        return elements;
	    }
	 
	 private static void checklinkKind(Document doc) throws Exception {
		 String[] tags = {"linkToSuccessor", "linkToPredecessor"};
		 int nbSuc=0;
		 int nbpred=0;
         List<Element> linkElements = getMultipleTagElements(doc, tags);
         for (Element elem : linkElements) {
	            //Element taskPrecedence = (Element) linkElements.item();
	            
	            // Check if the WorkSequenceKind attribute exists and is not empty
	            if (!elem.hasAttribute("linkKind") /*|| taskPrecedence.getAttribute("kind").trim().isEmpty()*/) {
	                throw new Exception("Missing or empty kind attribute in "+ elem.getTagName()+" at index: " + (elem.getTagName().equals("linkToSuccessor") ? nbSuc : nbpred));
	            }
	            
	            // Retrieve and trim the WorkSequenceKind attribute
	            String linkKind = elem.getAttribute("linkKind").trim();
	            if (!(linkKind.equals("FinishToStart") || linkKind.equals("StartToFinish") ||
	            		linkKind.equals("FinishToFinish") || linkKind.equals("StartToStart")|| linkKind.equals(""))) {
	            	
	            		throw new Exception("Invalid linkKind value: '" + linkKind + "' in "+elem.getTagName()+" at index: " +(elem.getTagName().equals("linkToSuccessor") ? nbSuc : nbpred));
	            	
	                
	            }
	        }
	    }
	 
	 private static Set<String> gatherReferencableIds(Document doc) {
	        Set<String> ids = new HashSet<>();
	        // Collect all Task names
	        NodeList tasks = doc.getElementsByTagName("Task");
	        for (int i = 0; i < tasks.getLength(); i++) {
	            Element task = (Element) tasks.item(i);
	            if (task.hasAttribute("name")) {
	                ids.add(task.getAttribute("name"));
	            }
	        }
	        // Collect all Product names
	        NodeList products = doc.getElementsByTagName("WorkProduct");
	        for (int i = 0; i < products.getLength(); i++) {
	            Element product = (Element) products.item(i);
	            if (product.hasAttribute("name")) {
	                ids.add(product.getAttribute("name"));
	            }
	        }
	        // Collect all Role names
	        NodeList roles = doc.getElementsByTagName("Role");
	        for (int i = 0; i < roles.getLength(); i++) {
	            Element role = (Element) roles.item(i);
	            if (role.hasAttribute("name")) {
	                ids.add(role.getAttribute("name"));
	            }
	        }
	        
	        NodeList tools = doc.getElementsByTagName("toolDefinition");
	        for (int i = 0; i < tools.getLength(); i++) {
	            Element tool = (Element) tools.item(i);
	            if (tool.hasAttribute("name")) {
	                ids.add(tool.getAttribute("name"));
	            }
	        }
	        return ids;
	    }

	    private static void checkReferences(Document doc, Set<String> referencableIds) throws Exception {
	        // Check all 'ref' attributes in the document to see if they refer to an existing id
	        NodeList refElements = doc.getElementsByTagName("*");
	        for (int i = 0; i < refElements.getLength(); i++) {
	        	//System.out.println(refElements.getLength()); 
	            Element element = (Element) refElements.item(i);
	            if (element.hasAttribute("ref")) {
	                String refValue = element.getAttribute("ref");
	                if (!referencableIds.contains(refValue)) {
	                    throw new Exception("Reference error:NO element with name '" + refValue + "' found in tag <" + element.getNodeName() + "> at index " + i);
	                }
	            }
	        }
	    }

	
	 
	
	public static void main(String[] args) {
		
		try {
		      
		String inputFile = "C:\\Users\\vappyq\\Documents\\ProjetM2\\ECPML\\modeles-in-XML\\exp3\\EXP3XMLimbrique.xml";

        // Load XML document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(inputFile));
        doc.getDocumentElement().normalize();
        
        NodeList tasks = doc.getElementsByTagName("Task");
        checkTasks(tasks);
            
        checkRoles(doc);
        
        checkProducts(doc);
        
        checkTools(doc);
        
        checkuses(doc);
        
        checkTaskPerformers(doc);
        
        checkTaskParameter(doc);
        
        checklinkKind(doc);
        
        Set<String> referencableIds = gatherReferencableIds(doc);

        // Check references
        checkReferences(doc, referencableIds);
        
        validateTags(doc);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}