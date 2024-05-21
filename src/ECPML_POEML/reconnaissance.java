package ECPML_POEML;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class reconnaissance {

    private Map<String, String[]> tagToLanguagesMap;

    public reconnaissance() {
        tagToLanguagesMap = new HashMap<>();
        // Example mappings
        String[] ecpmlTags = {"taskParameters", "taskParameter", "linkedTask", "WorkProduct", "linkToSuccessors",
                "taskPerformer", "taskPerformers", "Role", "Roles", "successor", "predecessor", "linkToPredecessors",
                "linkToPredecessor", "linkToSuccessor", "linkToPredecessor", "ImpactedProduct", "product", "ImpactedProducts",
                "Tasks", "Task", "ECPMLModel", "performer","toolsDefinition","toolDefinition","uses","use","managedTask","tool",
                "WorkProduct", "nestedProducts", "nestedProduct", "impactedProducts", "impactedProduct", "WorkProducts"};

        
        for (String tag : ecpmlTags) {
            tagToLanguagesMap.put(tag, new String[]{"ECPML"});
        }
        
        String[] poemlTags = {"TaskParameters", "TaskParameter", "LinkedTask", "Product",
                "TaskPerformance", "TaskPerformances", "Role", "Roles", "successor", "predecessor", "TaskPrecedence",
                "Aggregation", "Aggregations", "ProductImpact", "product", "Products", "ProductsImpact","sous-tasks","sous-task",
                "Tasks", "Task", "POEMLModel", "performer", "performedTask", "TaskPrecedences","ImpactedElement","ImpactingElement","aggregate","component"};
        
        for (String tag : poemlTags) {
            tagToLanguagesMap.put(tag, new String[]{"POEML"});
        }
        
        
      
               // Add more mappings as needed
    }

    public String[] getLanguagesForTag(String tag) {
        return tagToLanguagesMap.getOrDefault(tag, new String[]{});
    }

    public static void main(String[] args) {
        try {
            File inputFile = new File("C:\\Users\\vappyq\\Documents\\ProjetM2\\ECPML\\modeles-in-XML\\exp3\\output.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("*"); // Get all elements
            reconnaissance detector = new reconnaissance();
            Map<String, Integer> languageFrequencyMap = new HashMap<>();

            for (int i = 0; i < nList.getLength(); i++) {
                Node node = nList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String tagName = node.getNodeName();
                    String[] languages = detector.getLanguagesForTag(tagName);
                    for (String language : languages) {
                        languageFrequencyMap.put(language, languageFrequencyMap.getOrDefault(language, 0) + 1);
                    }
                }
            }

            // Determine language with the highest frequency
            String detectedLanguage = null;
            int maxFrequency = 0;
            for (Map.Entry<String, Integer> entry : languageFrequencyMap.entrySet()) {
                if (entry.getValue() > maxFrequency) {
                    maxFrequency = entry.getValue();
                    detectedLanguage = entry.getKey();
                }
            }

            System.out.println("Detected Language: " + detectedLanguage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
