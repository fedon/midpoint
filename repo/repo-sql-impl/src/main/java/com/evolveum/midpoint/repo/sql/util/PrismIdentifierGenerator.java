package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.cxf.common.util.StringUtils;

import java.util.*;

/**
 * @author lazyman
 */
public class PrismIdentifierGenerator {

    /**
     * Method inserts id for prism container values, which didn't have ids,
     * also returns all container values which has generated id
     *
     * @param object
     * @return
     */
    public IdGeneratorResult generate(PrismObject object) {
        IdGeneratorResult result = new IdGeneratorResult();
        if (StringUtils.isEmpty(object.getOid())) {
            String oid = UUID.randomUUID().toString();
            object.setOid(oid);

            result.setGeneratedOid(true);
        }

        generateIdForObject(object, result, result.isGeneratedOid());

        return result;
    }

    private void generateIdForObject(PrismObject object, IdGeneratorResult result, boolean generatedOid) {
        if (object == null) {
            return;
        }

        List<PrismContainer> containers = getChildrenContainers(object);
        Set<Long> usedIds = new HashSet<>();
        for (PrismContainer c : containers) {
            if (c == null || c.getValues() == null) {
                continue;
            }

            for (PrismContainerValue val : (List<PrismContainerValue>) c.getValues()) {
                if (val.getId() != null) {
                    usedIds.add(val.getId());
                }
            }
        }

        Long nextId = 1L;
        for (PrismContainer c : containers) {
            if (c == null || c.getValues() == null) {
                continue;
            }

            for (PrismContainerValue val : (List<PrismContainerValue>) c.getValues()) {
                if (val.getId() != null) {
                    if (generatedOid) {
                        result.getValues().add(val);
                    }

                    continue;
                }

                while (usedIds.contains(nextId)) {
                    nextId++;
                }

                val.setId(nextId);
                usedIds.add(nextId);

                result.getValues().add(val);
            }
        }
    }

    private List<PrismContainer> getChildrenContainers(PrismObject parent) {
        List<PrismContainer> containers = new ArrayList<>();
        if (ObjectType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            containers.add(parent.findContainer(ObjectType.F_TRIGGER));
        }

        if (FocusType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            containers.add(parent.findContainer(FocusType.F_ASSIGNMENT));
        }

        if (AbstractRoleType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            containers.add(parent.findContainer(AbstractRoleType.F_INDUCEMENT));
            containers.add(parent.findContainer(AbstractRoleType.F_EXCLUSION));
            containers.add(parent.findContainer(AbstractRoleType.F_AUTHORIZATION));
        }

        return containers;
    }
}
