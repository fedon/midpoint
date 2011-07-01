/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model.sync.action;

import java.util.List;

import javax.xml.bind.JAXBException;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author Vilo Repan
 */
public class UnlinkAccountAction extends BaseAction {

	private static Trace LOGGER = TraceManager.getTrace(UnlinkAccountAction.class);

	@Override
	public String executeChanges(String userOid, ResourceObjectShadowChangeDescriptionType change,
			SynchronizationSituationType situation, ResourceObjectShadowType shadowAfterChange,
			OperationResult result) throws SynchronizationException {
		LOGGER.trace("executeChanges::start");

		UserType userType = getUser(userOid, result);
		UserType oldUserType = null;
		try {
			oldUserType = (UserType) JAXBUtil.clone(userType);
		} catch (JAXBException ex) {
			// TODO: logging
			throw new SynchronizationException("Couldn't clone user.", ex);
		}
		ResourceObjectShadowType resourceShadow = change.getShadow();

		if (userType == null) {
			throw new SynchronizationException("Can't unlink account. User with oid '" + userOid
					+ "' doesn't exits. Try insert create action before this action.");
		}

		List<ObjectReferenceType> references = userType.getAccountRef();

		ObjectReferenceType accountRef = null;
		for (ObjectReferenceType reference : references) {
			if (!SchemaConstants.I_ACCOUNT_SHADOW_TYPE.equals(reference.getType())) {
				continue;
			}

			if (reference.getOid().equals(resourceShadow.getOid())) {
				accountRef = reference;
				break;
			}
		}
		if (accountRef != null) {
			LOGGER.debug("Removing account ref {} from user {}.",
					new Object[] { accountRef.getOid(), userOid });
			references.remove(accountRef);

			try {
				ObjectModificationType changes = CalculateXmlDiff.calculateChanges(oldUserType, userType);
				getModel().modifyObject(changes, result);
			} catch (DiffException ex) {
				LoggingUtils.logException(LOGGER, "Couldn't create user diff for {}", ex, userOid);
				throw new SynchronizationException("Couldn't create user diff for '" + userOid + "'.", ex);
			} catch (Exception ex) {
				throw new SynchronizationException("Can't unlink account. Can't save user", ex);
			}
		}

		LOGGER.trace("executeChanges::end");
		return userOid;
	}
}
