/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

package org.jbpm.bpmn2.xml.elements;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jbpm.workflow.core.node.DataAssociation;

public class ThrowEventWriter implements ElementWriter<List<DataAssociation>>{

    private DataInputAssociationWriter dataAssociationWriter;

    
    public ThrowEventWriter() {
        dataAssociationWriter = new DataInputAssociationWriter();
    }
    @Override
    public void write(OutputStream os, List<DataAssociation> dataAssociations) throws IOException {

        if(dataAssociations == null || dataAssociations.isEmpty()) {
            return;
        }

        Set<String> processed = new TreeSet<>();
        for(DataAssociation dataAssociation : dataAssociations) {
            if(processed.contains(dataAssociation.getTarget())) {
                continue;
            }
            os.write(("      <dataInput id=\"_" + dataAssociation.getUuid().toString() + "_"+ dataAssociation.getTarget() + "_Input\" name=\"" + dataAssociation.getTarget() + "\" />" + ElementConstants.EOL).getBytes());
            processed.add(dataAssociation.getTarget());

        }

        for(DataAssociation dataAssociation : dataAssociations) {
            dataAssociationWriter.write(os, dataAssociation);
        }
    }

}
