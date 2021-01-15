/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.bpmn2.xml;

import java.util.HashMap;
import java.util.Map;

import org.drools.core.xml.ExtensibleXmlParser;
import org.jbpm.bpmn2.core.ItemDefinition;
import org.jbpm.bpmn2.xml.elements.DataAssociationFactory;
import org.jbpm.bpmn2.xml.util.ProcessParserData;
import org.jbpm.process.core.Work;
import org.jbpm.process.core.impl.WorkImpl;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.core.NodeContainer;
import org.jbpm.workflow.core.impl.NodeImpl;
import org.jbpm.workflow.core.node.ForEachNode;
import org.jbpm.workflow.core.node.MilestoneNode;
import org.jbpm.workflow.core.node.WorkItemNode;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TaskHandler extends AbstractNodeHandler {
    

	
	 Map<String, String> dataTypeInputs = new HashMap<String, String>();
     Map<String, String> dataTypeOutputs = new HashMap<String, String>();

    protected Node createNode(Attributes attrs) {
        return new WorkItemNode();
    }
    
	public Class<?> generateNodeFor() {
        return Node.class;
    }

    protected void handleNode(final Node node, final Element element, final String uri, 
            final String localName, final ExtensibleXmlParser parser) throws SAXException {
    	super.handleNode(node, element, uri, localName, parser);
        ProcessParserData processData = ProcessParserData.wrapParserMetadata(parser);
    	dataTypeInputs.clear();
    	dataTypeOutputs.clear();
    	
    	WorkItemNode workItemNode = (WorkItemNode) node;
        String name = getTaskName(element);
        Work work = new WorkImpl();
        work.setName(name);
    	workItemNode.setWork(work);
    	org.w3c.dom.Node xmlNode = element.getFirstChild();
        while (xmlNode != null) {
        	String nodeName = xmlNode.getNodeName();
        	if ("ioSpecification".equals(nodeName)) {
        		readIoSpecification(processData, xmlNode, dataInputs, dataOutputs);
        	} else if ("dataInputAssociation".equals(nodeName)) {
        		readDataInputAssociation(xmlNode, workItemNode, dataInputs);
        	} else if ("dataOutputAssociation".equals(nodeName)) {
        		readDataOutputAssociation(xmlNode, workItemNode, dataOutputs);
        	}
    		xmlNode = xmlNode.getNextSibling();
        }
        workItemNode.setMetaData("DataInputs", new HashMap<String, String>(dataTypeInputs) );
        workItemNode.setMetaData("DataOutputs", new HashMap<String, String>(dataTypeOutputs) );
        handleScript(workItemNode, element, "onEntry");
        handleScript(workItemNode, element, "onExit");
        
        String compensation = element.getAttribute("isForCompensation");
        if( compensation != null ) {
            boolean isForCompensation = Boolean.parseBoolean(compensation);
            if( isForCompensation ) { 
                workItemNode.setMetaData("isForCompensation", isForCompensation );
            }
        }  
	}
    
    protected String getTaskName(final Element element) {
        return element.getAttribute("taskName");
    }
    
    protected void readIoSpecification(ProcessParserData processData, org.w3c.dom.Node xmlNode, Map<String, String> dataInputs, Map<String, String> dataOutputs) {
        Map<String, ItemDefinition> itemDefinitions = processData.itemDefinitions.get();
        dataTypeInputs.clear();
        dataTypeOutputs.clear();
        
        org.w3c.dom.Node subNode = xmlNode.getFirstChild();
        while (subNode instanceof Element) {
            String subNodeName = subNode.getNodeName();
            if ("dataInput".equals(subNodeName)) {
                String id = ((Element) subNode).getAttribute("id");
                String inputName = ((Element) subNode).getAttribute("name");
                dataInputs.put(id, inputName);
                
                String itemSubjectRef = ((Element) subNode).getAttribute("itemSubjectRef");                
                if (itemSubjectRef == null || itemSubjectRef.isEmpty()) {
                    String dataType = ((Element) subNode).getAttribute("dtype");
                    if (dataType == null || dataType.isEmpty()) {
                        dataType = "java.lang.String";
                    }
                    dataTypeInputs.put(inputName, dataType);
                } else if (itemDefinitions.get(itemSubjectRef) != null) {
                    dataTypeInputs.put(inputName, itemDefinitions.get(itemSubjectRef).getStructureRef());
                } else {
                    dataTypeInputs.put(inputName, "java.lang.Object");
                }
            }
            if ("dataOutput".equals(subNodeName)) {
                String id = ((Element) subNode).getAttribute("id");
                String outputName = ((Element) subNode).getAttribute("name");
                dataOutputs.put(id, outputName);
                
                String itemSubjectRef = ((Element) subNode).getAttribute("itemSubjectRef");
                
                if (itemSubjectRef == null || itemSubjectRef.isEmpty()) {
                    String dataType = ((Element) subNode).getAttribute("dtype");
                    if (dataType == null || dataType.isEmpty()) {
                        dataType = "java.lang.String";
                    }
                    dataTypeOutputs.put(outputName, dataType);
                } else if (itemDefinitions.get(itemSubjectRef) != null) {
                    dataTypeOutputs.put(outputName, itemDefinitions.get(itemSubjectRef).getStructureRef());
                } else {
                    dataTypeOutputs.put(outputName, "java.lang.Object");
                }
            }
            subNode = subNode.getNextSibling();
        }
    }

    protected void readDataInputAssociation(org.w3c.dom.Node xmlNode, WorkItemNode workItemNode, Map<String, String> dataInputs) {
        DataAssociationFactory.readDataInputAssociation(xmlNode, dataInputs, DataAssociationFactory::isLegacyAssignment,
                workItemNode::addInAssociation, (name, value) -> workItemNode.getWork().setParameter(name, value));
    }

    protected void readDataOutputAssociation(org.w3c.dom.Node xmlNode, WorkItemNode workItemNode, Map<String, String> dataOutputs) {
        workItemNode.addOutAssociation(DataAssociationFactory.readDataOutputAssociation(xmlNode, dataOutputs));
    }

    @Override
    public void writeNode(Node node, StringBuilder xmlDump, int metaDataType) {
        throw new IllegalArgumentException(
            "Writing out should be handled by the WorkItemNodeHandler");
    }
    
    public Object end(final String uri, final String localName,
            final ExtensibleXmlParser parser) throws SAXException {
        ProcessParserData processData = ProcessParserData.wrapParserMetadata(parser);
		final Element element = parser.endElementBuilder();
		Node node = processData.<Node>current();
		// determine type of event definition, so the correct type of node can be generated
    	handleNode(node, element, uri, localName, parser);
    	
		org.w3c.dom.Node xmlNode = element.getFirstChild();
		int uniqueIdGen = 1;
		while (xmlNode != null) {
			String nodeName = xmlNode.getNodeName();
			if ("multiInstanceLoopCharacteristics".equals(nodeName)) {
				// create new timerNode
				ForEachNode forEachNode = new ForEachNode();
				forEachNode.setId(node.getId());
				String uniqueId = (String) node.getMetaData().get("UniqueId");
				forEachNode.setMetaData("UniqueId", uniqueId);
				node.setMetaData("UniqueId", uniqueId + ":" + uniqueIdGen++);
				forEachNode.addNode(node);
				forEachNode.linkIncomingConnections(NodeImpl.CONNECTION_DEFAULT_TYPE, node.getId(), NodeImpl.CONNECTION_DEFAULT_TYPE);
				forEachNode.linkOutgoingConnections(node.getId(), NodeImpl.CONNECTION_DEFAULT_TYPE, NodeImpl.CONNECTION_DEFAULT_TYPE);
				forEachNode.setSequential(Boolean.parseBoolean(((Element) xmlNode).getAttribute("isSequential")));
				
				Node orignalNode = node;				
				node = forEachNode;
				handleForEachNode(node, element, uri, localName, parser);
				// remove output collection data output of for each to avoid problems when running in variable strict mode
				if (orignalNode instanceof WorkItemNode && forEachNode.getOutputCollectionExpression() != null) {
					((WorkItemNode)orignalNode).adjustOutMapping(forEachNode.getOutputCollectionExpression());
				}
								
				break;
			}
			xmlNode = xmlNode.getNextSibling();
		}
		// replace node in case it's milestone
		if (node instanceof WorkItemNode && ((WorkItemNode)node).getWork().getName().equals("Milestone")) {
		    WorkItemNode workItemNode = (WorkItemNode) node;
		    
		    String milestoneCondition = (String)((WorkItemNode)node).getWork().getParameter("Condition");
		    if (milestoneCondition == null) {
		        milestoneCondition = "";// if not given that means once activated it's achieved
		    }
		    
		    MilestoneNode milestoneNode = new MilestoneNode();
		    milestoneNode.setId(workItemNode.getId());
		    milestoneNode.setConstraint(milestoneCondition);
		    milestoneNode.setMatchVariable((String)((WorkItemNode)node).getWork().getParameter("MatchVariable"));
		    milestoneNode.setMetaData(workItemNode.getMetaData());
		    milestoneNode.setName(workItemNode.getName());
		    milestoneNode.setNodeContainer(workItemNode.getNodeContainer());
		    
		    node = milestoneNode;
		}
		
		NodeContainer nodeContainer = (NodeContainer) parser.getParent();
		nodeContainer.addNode(node);
		processData.nodes.add(node);
		return node;
	}
    
	protected void handleForEachNode(final Node node, final Element element, final String uri, 
            final String localName, final ExtensibleXmlParser parser) throws SAXException {
    	ForEachNode forEachNode = (ForEachNode) node;
    	org.w3c.dom.Node xmlNode = element.getFirstChild();
    	
        while (xmlNode != null) {
            String nodeName = xmlNode.getNodeName();
            if ("dataInputAssociation".equals(nodeName)) {
                readDataInputAssociation(xmlNode, inputAssociation);
            } else if ("dataOutputAssociation".equals(nodeName)) {
                readDataOutputAssociation(xmlNode, outputAssociation);
            } else if ("multiInstanceLoopCharacteristics".equals(nodeName)) {
            	readMultiInstanceLoopCharacteristics(xmlNode, forEachNode, parser);
            }
            xmlNode = xmlNode.getNextSibling();
        }
    }



}
