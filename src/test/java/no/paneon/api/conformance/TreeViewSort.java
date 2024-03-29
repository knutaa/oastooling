package no.paneon.api.conformance;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import no.paneon.api.conformance2.Sorter;
import no.paneon.api.utils.Out;

public class TreeViewSort {


	
	static List<String> labels = Arrays.asList(

	"Service.externalIdentifier.id",
	"Service.externalIdentifier.@type",
	"Service.feature.name",
	"Service.feature.@type",
	"Service.feature.featureCharacteristic.name",
	"Service.feature.featureCharacteristic.@type",
	"Service.feature.featureCharacteristic.characteristicRelationship.id",
	"Service.feature.featureCharacteristic.characteristicRelationship.relationshipType",
	"Service.feature.featureCharacteristic.characteristicRelationship.@type",
	"Service.feature.featureRelationship.id",
	"Service.feature.featureRelationship.relationshipType",
	"Service.feature.featureRelationship.@type",
	"Service.feature.policyConstraint.id",
	"Service.feature.policyConstraint.@type",
	"Service.intent.creationDate",
	"Service.intent.expression",
	"Service.intent.id",
	"Service.intent.lastUpdate",
	"Service.intent.lifecycleStatus",
	"Service.intent.name",
	"Service.intent.@type",
	"Service.intent.attachment.attachmentType",
	"Service.intent.attachment.id",
	"Service.intent.attachment.mimeType",
	"Service.intent.attachment.@type",
	"Service.intent.characteristic.name",
	"Service.intent.characteristic.@type",
	"Service.intent.characteristic.characteristicRelationship.id",
	"Service.intent.characteristic.characteristicRelationship.relationshipType",
	"Service.intent.characteristic.characteristicRelationship.@type",
	"Service.intent.expression.expressionValue",
	"Service.intent.expression.@type",
	"Service.intent.intentRelationship.id",
	"Service.intent.intentRelationship.relationshipType",
	"Service.intent.intentRelationship.@referredType",
	"Service.intent.intentRelationship.@type",
	"Service.intent.intentRelationship.associationSpec.id",
	"Service.intent.intentRelationship.associationSpec.@type",
	"Service.intent.intentSpecification.id",
	"Service.intent.intentSpecification.@type",
	"Service.intent.relatedParty.role",
	"Service.intent.relatedParty.@type",
	"Service.intent.relatedParty.partyOrPartyRole.id",
	"Service.intent.relatedParty.partyOrPartyRole.@type",
	"Service.note.@type",
	"Service.place.place",
	"Service.place.role",
	"Service.place.@type",
	"Service.place.place.id",
	"Service.place.place.@type",
	"Service.place.place.calendar.status",
	"Service.place.place.calendar.@type",
	"Service.place.place.calendar.hourPeriod.@type",
	"Service.place.place.countryCode.href",
	"Service.place.place.countryCode.@type",
	"Service.place.place.externalIdentifier.id",
	"Service.place.place.externalIdentifier.@type",
	"Service.place.place.geographicLocation.id",
	"Service.place.place.geographicLocation.@type",
	"Service.place.place.geographicSubAddress.@type",
	"Service.place.place.geographicSubAddress.subUnit.subUnitNumber",
	"Service.place.place.geographicSubAddress.subUnit.subUnitType",
	"Service.place.place.geographicSubAddress.subUnit.@type",
	"Service.place.place.relatedParty.role",
	"Service.place.place.relatedParty.@type",
	"Service.place.place.relatedParty.partyOrPartyRole.id",
	"Service.place.place.relatedParty.partyOrPartyRole.@type",
	"Service.place.place.siteRelationship.id",
	"Service.place.place.siteRelationship.relationshipType",
	"Service.place.place.siteRelationship.@type",
	"Service.relatedEntity.entity",
	"Service.relatedEntity.role",
	"Service.relatedEntity.@type",
	"Service.relatedEntity.entity.id",
	"Service.relatedEntity.entity.@type",
	"Service.relatedParty.role",
	"Service.relatedParty.@type",
	"Service.relatedParty.partyOrPartyRole.id",
	"Service.relatedParty.partyOrPartyRole.@type",
	"Service.serviceCharacteristic.name",
	"Service.serviceCharacteristic.@type",
	"Service.serviceCharacteristic.characteristicRelationship.id",
	"Service.serviceCharacteristic.characteristicRelationship.relationshipType",
	"Service.serviceCharacteristic.characteristicRelationship.@type",
	"Service.serviceOrderItem.@type",
	"Service.serviceRelationship.relationshipType",
	"Service.serviceRelationship.@type",
	"Service.serviceRelationship.service.id",
	"Service.serviceRelationship.service.serviceSpecification",
	"Service.serviceRelationship.service.state",
	"Service.serviceRelationship.service.@type",
	"Service.serviceRelationship.service.externalIdentifier.id",
	"Service.serviceRelationship.service.externalIdentifier.@type",
	"Service.serviceRelationship.service.feature.name",
	"Service.serviceRelationship.service.feature.@type",
	"Service.serviceRelationship.service.feature.featureCharacteristic.name",
	"Service.serviceRelationship.service.feature.featureCharacteristic.@type",
	"Service.serviceRelationship.service.feature.featureCharacteristic.characteristicRelationship.id",
	"Service.serviceRelationship.service.feature.featureCharacteristic.characteristicRelationship.relationshipType",
	"Service.serviceRelationship.service.feature.featureCharacteristic.characteristicRelationship.@type",
	"Service.serviceRelationship.service.feature.featureRelationship.id",
	"Service.serviceRelationship.service.feature.featureRelationship.relationshipType",
	"Service.serviceRelationship.service.feature.featureRelationship.@type",
	"Service.serviceRelationship.service.feature.policyConstraint.id",
	"Service.serviceRelationship.service.feature.policyConstraint.@type",
	"Service.serviceRelationship.service.intent.expression",
	"Service.serviceRelationship.service.intent.id",
	"Service.serviceRelationship.service.intent.name",
	"Service.serviceRelationship.service.intent.@type",
	"Service.serviceRelationship.service.intent.attachment.attachmentType",
	"Service.serviceRelationship.service.intent.attachment.id",
	"Service.serviceRelationship.service.intent.attachment.mimeType",
	"Service.serviceRelationship.service.intent.attachment.@type",
	"Service.serviceRelationship.service.intent.characteristic.name",
	"Service.serviceRelationship.service.intent.characteristic.@type",
	"Service.serviceRelationship.service.intent.characteristic.characteristicRelationship.id",
	"Service.serviceRelationship.service.intent.characteristic.characteristicRelationship.relationshipType",
	"Service.serviceRelationship.service.intent.characteristic.characteristicRelationship.@type",
	"Service.serviceRelationship.service.intent.expression.expressionValue",
	"Service.serviceRelationship.service.intent.expression.@type",
	"Service.serviceRelationship.service.intent.intentRelationship.id",
	"Service.serviceRelationship.service.intent.intentRelationship.relationshipType",
	"Service.serviceRelationship.service.intent.intentRelationship.@referredType",
	"Service.serviceRelationship.service.intent.intentRelationship.@type",
	"Service.serviceRelationship.service.intent.intentRelationship.associationSpec.id",
	"Service.serviceRelationship.service.intent.intentRelationship.associationSpec.@type",
	"Service.serviceRelationship.service.intent.intentSpecification.id",
	"Service.serviceRelationship.service.intent.intentSpecification.@type",
	"Service.serviceRelationship.service.intent.relatedParty.role",
	"Service.serviceRelationship.service.intent.relatedParty.@type",
	"Service.serviceRelationship.service.intent.relatedParty.partyOrPartyRole.id",
	"Service.serviceRelationship.service.intent.relatedParty.partyOrPartyRole.@type",
	"Service.serviceRelationship.service.note.@type",
	"Service.serviceRelationship.service.place.place",
	"Service.serviceRelationship.service.place.role",
	"Service.serviceRelationship.service.place.@type",
	"Service.serviceRelationship.service.place.place.id",
	"Service.serviceRelationship.service.place.place.@type",
	"Service.serviceRelationship.service.place.place.calendar.status", 
	"Service.serviceRelationship.service.place.place.calendar.@type",
	"Service.serviceRelationship.service.place.place.calendar.hourPeriod.@type",
	"Service.serviceRelationship.service.place.place.countryCode.href",
	"Service.serviceRelationship.service.place.place.countryCode.@type",
	"Service.serviceRelationship.service.place.place.externalIdentifier.id",
	"Service.serviceRelationship.service.place.place.externalIdentifier.@type",
	"Service.serviceRelationship.service.place.place.geographicLocation.id",
	"Service.serviceRelationship.service.place.place.geographicLocation.@type",
	"Service.serviceRelationship.service.place.place.geographicSubAddress.@type",
	"Service.serviceRelationship.service.place.place.geographicSubAddress.subUnit.subUnitNumber",
	"Service.serviceRelationship.service.place.place.geographicSubAddress.subUnit.subUnitType",
	"Service.serviceRelationship.service.place.place.geographicSubAddress.subUnit.@type",
	"Service.serviceRelationship.service.place.place.relatedParty.role",
	"Service.serviceRelationship.service.place.place.relatedParty.@type",
	"Service.serviceRelationship.service.place.place.relatedParty.partyOrPartyRole.id",
	"Service.serviceRelationship.service.place.place.relatedParty.partyOrPartyRole.@type",
	"Service.serviceRelationship.service.place.place.siteRelationship.id",
	"Service.serviceRelationship.service.place.place.siteRelationship.relationshipType",
	"Service.serviceRelationship.service.place.place.siteRelationship.@type",
	"Service.serviceRelationship.service.relatedEntity.entity",
	"Service.serviceRelationship.service.relatedEntity.role",
	"Service.serviceRelationship.service.relatedEntity.@type",
	"Service.serviceRelationship.service.relatedEntity.entity.id",
	"Service.serviceRelationship.service.relatedEntity.entity.@type",
	"Service.serviceRelationship.service.relatedParty.role",
	"Service.serviceRelationship.service.relatedParty.@type",
	"Service.serviceRelationship.service.relatedParty.partyOrPartyRole.id",
	"Service.serviceRelationship.service.relatedParty.partyOrPartyRole.@type",
	"Service.serviceRelationship.service.serviceCharacteristic.name",
	"Service.serviceRelationship.service.serviceCharacteristic.@type",
	"Service.serviceRelationship.service.serviceCharacteristic.characteristicRelationship.id",
	"Service.serviceRelationship.service.serviceCharacteristic.characteristicRelationship.relationshipType",
	"Service.serviceRelationship.service.serviceCharacteristic.characteristicRelationship.@type",
	"Service.serviceRelationship.service.serviceOrderItem.@type",
	"Service.serviceRelationship.service.serviceSpecification.id",
	"Service.serviceRelationship.service.serviceSpecification.@type",
	"Service.serviceRelationship.service.supportingResource.id",
	"Service.serviceRelationship.service.supportingResource.@type",
	"Service.serviceRelationship.serviceRelationshipCharacteristic.name",
	"Service.serviceRelationship.serviceRelationshipCharacteristic.@type",
	"Service.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.id",
	"Service.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.relationshipType",
	"Service.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.@type",
	"Service.supportingResource.id",
	"Service.supportingResource.@type",
	"Service.supportingService.id",
	"Service.supportingService.serviceSpecification",
	"Service.supportingService.state",
	"Service.supportingService.@type",
	"Service.supportingService.externalIdentifier.id",
	"Service.supportingService.externalIdentifier.@type",
	"Service.supportingService.feature.name",
	"Service.supportingService.feature.@type",
	"Service.supportingService.feature.featureCharacteristic.name",
	"Service.supportingService.feature.featureCharacteristic.@type",
	"Service.supportingService.feature.featureCharacteristic.characteristicRelationship.id",
	"Service.supportingService.feature.featureCharacteristic.characteristicRelationship.relationshipType",
	"Service.supportingService.feature.featureCharacteristic.characteristicRelationship.@type",
	"Service.supportingService.feature.featureRelationship.id",
	"Service.supportingService.feature.featureRelationship.relationshipType",
	"Service.supportingService.feature.featureRelationship.@type",
	"Service.supportingService.feature.policyConstraint.id",
	"Service.supportingService.feature.policyConstraint.@type",
	"Service.supportingService.intent.expression",
	"Service.supportingService.intent.id",
	"Service.supportingService.intent.name",
	"Service.supportingService.intent.@type",
	"Service.supportingService.intent.attachment.attachmentType",
	"Service.supportingService.intent.attachment.id",
	"Service.supportingService.intent.attachment.mimeType",
	"Service.supportingService.intent.attachment.@type",
	"Service.supportingService.intent.characteristic.name",
	"Service.supportingService.intent.characteristic.@type",
	"Service.supportingService.intent.characteristic.characteristicRelationship.id",
	"Service.supportingService.intent.characteristic.characteristicRelationship.relationshipType",
	"Service.supportingService.intent.characteristic.characteristicRelationship.@type",
	"Service.supportingService.intent.expression.expressionValue",
	"Service.supportingService.intent.expression.@type",
	"Service.supportingService.intent.intentRelationship.id",
	"Service.supportingService.intent.intentRelationship.relationshipType",
	"Service.supportingService.intent.intentRelationship.@referredType",
	"Service.supportingService.intent.intentRelationship.@type",
	"Service.supportingService.intent.intentRelationship.associationSpec.id",
	"Service.supportingService.intent.intentRelationship.associationSpec.@type",
	"Service.supportingService.intent.intentSpecification.id",
	"Service.supportingService.intent.intentSpecification.@type",
	"Service.supportingService.intent.relatedParty.role",
	"Service.supportingService.intent.relatedParty.@type",
	"Service.supportingService.intent.relatedParty.partyOrPartyRole.id",
	"Service.supportingService.intent.relatedParty.partyOrPartyRole.@type",
	"Service.supportingService.note.@type",
	"Service.supportingService.place.place",
	"Service.supportingService.place.role",
	"Service.supportingService.place.@type",
	"Service.supportingService.place.place.id",
	"Service.supportingService.place.place.@type",
	"Service.supportingService.place.place.calendar.status",
	"Service.supportingService.place.place.calendar.@type",
	"Service.supportingService.place.place.calendar.hourPeriod.@type",
	"Service.supportingService.place.place.countryCode.href",
	"Service.supportingService.place.place.countryCode.@type",
	"Service.supportingService.place.place.externalIdentifier.id",
	"Service.supportingService.place.place.externalIdentifier.@type",
	"Service.supportingService.place.place.geographicLocation.id",
	"Service.supportingService.place.place.geographicLocation.@type",
	"Service.supportingService.place.place.geographicSubAddress.@type",
	"Service.supportingService.place.place.geographicSubAddress.subUnit.subUnitNumber",
	"Service.supportingService.place.place.geographicSubAddress.subUnit.subUnitType",
	"Service.supportingService.place.place.geographicSubAddress.subUnit.@type",
	"Service.supportingService.place.place.relatedParty.role",
	"Service.supportingService.place.place.relatedParty.@type",
	"Service.supportingService.place.place.relatedParty.partyOrPartyRole.id",
	"Service.supportingService.place.place.relatedParty.partyOrPartyRole.@type",
	"Service.supportingService.place.place.siteRelationship.id",
	"Service.supportingService.place.place.siteRelationship.relationshipType",
	"Service.supportingService.place.place.siteRelationship.@type",
	"Service.supportingService.relatedEntity.entity",
	"Service.supportingService.relatedEntity.role",
	"Service.supportingService.relatedEntity.@type",
	"Service.supportingService.relatedEntity.entity.id",
	"Service.supportingService.relatedEntity.entity.@type",
	"Service.supportingService.relatedParty.role",
	"Service.supportingService.relatedParty.@type",
	"Service.supportingService.relatedParty.partyOrPartyRole.id",
	"Service.supportingService.relatedParty.partyOrPartyRole.@type",
	"Service.supportingService.serviceCharacteristic.name",
	"Service.supportingService.serviceCharacteristic.@type",
	"Service.supportingService.serviceCharacteristic.characteristicRelationship.id",
	"Service.supportingService.serviceCharacteristic.characteristicRelationship.relationshipType",
	"Service.supportingService.serviceCharacteristic.characteristicRelationship.@type",
	"Service.supportingService.serviceOrderItem.@type",
	"Service.supportingService.serviceRelationship.relationshipType",
	"Service.supportingService.serviceRelationship.@type",
	"Service.supportingService.serviceRelationship.serviceRelationshipCharacteristic.name",
	"Service.supportingService.serviceRelationship.serviceRelationshipCharacteristic.@type",
	"Service.supportingService.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.id",
	"Service.supportingService.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.relationshipType",
	"Service.supportingService.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.@type",
	"Service.supportingService.serviceSpecification.id",
	"Service.supportingService.serviceSpecification.@type",
	"Service.supportingService.supportingResource.id",
	"Service.supportingService.supportingResource.@type"

	);
	
	
	static List<String> expected_result = Arrays.asList(
			"Service.externalIdentifier.id",
			"Service.externalIdentifier.@type",
			"Service.feature.@type",
			"Service.feature.name",
			"Service.feature.featureCharacteristic.@type",
			"Service.feature.featureCharacteristic.name",
			"Service.feature.featureCharacteristic.characteristicRelationship.id",
			"Service.feature.featureCharacteristic.characteristicRelationship.relationshipType",
			"Service.feature.featureCharacteristic.characteristicRelationship.@type",
			"Service.feature.featureRelationship.id",
			"Service.feature.featureRelationship.relationshipType",
			"Service.feature.featureRelationship.@type",
			"Service.feature.policyConstraint.id",
			"Service.feature.policyConstraint.@type",
			"Service.intent.@type",
			"Service.intent.creationDate",
			"Service.intent.id",
			"Service.intent.lastUpdate",
			"Service.intent.lifecycleStatus",
			"Service.intent.name",
			"Service.intent.attachment.attachmentType",
			"Service.intent.attachment.id",
			"Service.intent.attachment.mimeType",
			"Service.intent.attachment.@type",
			"Service.intent.characteristic.@type",
			"Service.intent.characteristic.name",
			"Service.intent.characteristic.characteristicRelationship.id",
			"Service.intent.characteristic.characteristicRelationship.relationshipType",
			"Service.intent.characteristic.characteristicRelationship.@type",
			"Service.intent.expression",
			"Service.intent.expression.expressionValue",
			"Service.intent.expression.@type",
			"Service.intent.intentRelationship.@referredType",
			"Service.intent.intentRelationship.@type",
			"Service.intent.intentRelationship.id",
			"Service.intent.intentRelationship.relationshipType",
			"Service.intent.intentRelationship.associationSpec.id",
			"Service.intent.intentRelationship.associationSpec.@type",
			"Service.intent.intentSpecification.id",
			"Service.intent.intentSpecification.@type",
			"Service.intent.relatedParty.@type",
			"Service.intent.relatedParty.role",
			"Service.intent.relatedParty.partyOrPartyRole.id",
			"Service.intent.relatedParty.partyOrPartyRole.@type",
			"Service.note.@type",
			"Service.place.@type",
			"Service.place.role",
			"Service.place.place",
			"Service.place.place.@type",
			"Service.place.place.id",
			"Service.place.place.calendar.@type",
			"Service.place.place.calendar.status",
			"Service.place.place.calendar.hourPeriod.@type",
			"Service.place.place.countryCode.href",
			"Service.place.place.countryCode.@type",
			"Service.place.place.externalIdentifier.id",
			"Service.place.place.externalIdentifier.@type",
			"Service.place.place.geographicLocation.id",
			"Service.place.place.geographicLocation.@type",
			"Service.place.place.geographicSubAddress.@type",
			"Service.place.place.geographicSubAddress.subUnit.subUnitNumber",
			"Service.place.place.geographicSubAddress.subUnit.subUnitType",
			"Service.place.place.geographicSubAddress.subUnit.@type",
			"Service.place.place.relatedParty.@type",
			"Service.place.place.relatedParty.role",
			"Service.place.place.relatedParty.partyOrPartyRole.id",
			"Service.place.place.relatedParty.partyOrPartyRole.@type",
			"Service.place.place.siteRelationship.id",
			"Service.place.place.siteRelationship.relationshipType",
			"Service.place.place.siteRelationship.@type",
			"Service.relatedEntity.@type",
			"Service.relatedEntity.role",
			"Service.relatedEntity.entity",
			"Service.relatedEntity.entity.id",
			"Service.relatedEntity.entity.@type",
			"Service.relatedParty.@type",
			"Service.relatedParty.role",
			"Service.relatedParty.partyOrPartyRole.id",
			"Service.relatedParty.partyOrPartyRole.@type",
			"Service.serviceCharacteristic.@type",
			"Service.serviceCharacteristic.name",
			"Service.serviceCharacteristic.characteristicRelationship.id",
			"Service.serviceCharacteristic.characteristicRelationship.relationshipType",
			"Service.serviceCharacteristic.characteristicRelationship.@type",
			"Service.serviceOrderItem.@type",
			"Service.serviceRelationship.@type",
			"Service.serviceRelationship.relationshipType",
			"Service.serviceRelationship.service.@type",
			"Service.serviceRelationship.service.id",
			"Service.serviceRelationship.service.state",
			"Service.serviceRelationship.serviceRelationshipCharacteristic.@type",
			"Service.serviceRelationship.serviceRelationshipCharacteristic.name",
			"Service.serviceRelationship.service.externalIdentifier.id",
			"Service.serviceRelationship.service.externalIdentifier.@type",
			"Service.serviceRelationship.service.feature.@type",
			"Service.serviceRelationship.service.feature.name",
			"Service.serviceRelationship.service.feature.featureCharacteristic.@type",
			"Service.serviceRelationship.service.feature.featureCharacteristic.name",
			"Service.serviceRelationship.service.feature.featureCharacteristic.characteristicRelationship.id",
			"Service.serviceRelationship.service.feature.featureCharacteristic.characteristicRelationship.relationshipType",
			"Service.serviceRelationship.service.feature.featureCharacteristic.characteristicRelationship.@type",
			"Service.serviceRelationship.service.feature.featureRelationship.id",
			"Service.serviceRelationship.service.feature.featureRelationship.relationshipType",
			"Service.serviceRelationship.service.feature.featureRelationship.@type",
			"Service.serviceRelationship.service.feature.policyConstraint.id",
			"Service.serviceRelationship.service.feature.policyConstraint.@type",
			"Service.serviceRelationship.service.intent.@type",
			"Service.serviceRelationship.service.intent.id",
			"Service.serviceRelationship.service.intent.name",
			"Service.serviceRelationship.service.intent.attachment.attachmentType",
			"Service.serviceRelationship.service.intent.attachment.id",
			"Service.serviceRelationship.service.intent.attachment.mimeType",
			"Service.serviceRelationship.service.intent.attachment.@type",
			"Service.serviceRelationship.service.intent.characteristic.@type",
			"Service.serviceRelationship.service.intent.characteristic.name",
			"Service.serviceRelationship.service.intent.characteristic.characteristicRelationship.id",
			"Service.serviceRelationship.service.intent.characteristic.characteristicRelationship.relationshipType",
			"Service.serviceRelationship.service.intent.characteristic.characteristicRelationship.@type",
			"Service.serviceRelationship.service.intent.expression",
			"Service.serviceRelationship.service.intent.expression.expressionValue",
			"Service.serviceRelationship.service.intent.expression.@type",
			"Service.serviceRelationship.service.intent.intentRelationship.@referredType",
			"Service.serviceRelationship.service.intent.intentRelationship.@type",
			"Service.serviceRelationship.service.intent.intentRelationship.id",
			"Service.serviceRelationship.service.intent.intentRelationship.relationshipType",
			"Service.serviceRelationship.service.intent.intentRelationship.associationSpec.id",
			"Service.serviceRelationship.service.intent.intentRelationship.associationSpec.@type",
			"Service.serviceRelationship.service.intent.intentSpecification.id",
			"Service.serviceRelationship.service.intent.intentSpecification.@type",
			"Service.serviceRelationship.service.intent.relatedParty.@type",
			"Service.serviceRelationship.service.intent.relatedParty.role",
			"Service.serviceRelationship.service.intent.relatedParty.partyOrPartyRole.id",
			"Service.serviceRelationship.service.intent.relatedParty.partyOrPartyRole.@type",
			"Service.serviceRelationship.service.note.@type",
			"Service.serviceRelationship.service.place.@type",
			"Service.serviceRelationship.service.place.role",
			"Service.serviceRelationship.service.place.place",
			"Service.serviceRelationship.service.place.place.@type",
			"Service.serviceRelationship.service.place.place.id",
			"Service.serviceRelationship.service.place.place.calendar.@type",
			"Service.serviceRelationship.service.place.place.calendar.status",
			"Service.serviceRelationship.service.place.place.calendar.hourPeriod.@type",
			"Service.serviceRelationship.service.place.place.countryCode.href",
			"Service.serviceRelationship.service.place.place.countryCode.@type",
			"Service.serviceRelationship.service.place.place.externalIdentifier.id",
			"Service.serviceRelationship.service.place.place.externalIdentifier.@type",
			"Service.serviceRelationship.service.place.place.geographicLocation.id",
			"Service.serviceRelationship.service.place.place.geographicLocation.@type",
			"Service.serviceRelationship.service.place.place.geographicSubAddress.@type",
			"Service.serviceRelationship.service.place.place.geographicSubAddress.subUnit.subUnitNumber",
			"Service.serviceRelationship.service.place.place.geographicSubAddress.subUnit.subUnitType",
			"Service.serviceRelationship.service.place.place.geographicSubAddress.subUnit.@type",
			"Service.serviceRelationship.service.place.place.relatedParty.@type",
			"Service.serviceRelationship.service.place.place.relatedParty.role",
			"Service.serviceRelationship.service.place.place.relatedParty.partyOrPartyRole.id",
			"Service.serviceRelationship.service.place.place.relatedParty.partyOrPartyRole.@type",
			"Service.serviceRelationship.service.place.place.siteRelationship.id",
			"Service.serviceRelationship.service.place.place.siteRelationship.relationshipType",
			"Service.serviceRelationship.service.place.place.siteRelationship.@type",
			"Service.serviceRelationship.service.relatedEntity.@type",
			"Service.serviceRelationship.service.relatedEntity.role",
			"Service.serviceRelationship.service.relatedEntity.entity",
			"Service.serviceRelationship.service.relatedEntity.entity.id",
			"Service.serviceRelationship.service.relatedEntity.entity.@type",
			"Service.serviceRelationship.service.relatedParty.@type",
			"Service.serviceRelationship.service.relatedParty.role",
			"Service.serviceRelationship.service.relatedParty.partyOrPartyRole.id",
			"Service.serviceRelationship.service.relatedParty.partyOrPartyRole.@type",
			"Service.serviceRelationship.service.serviceCharacteristic.@type",
			"Service.serviceRelationship.service.serviceCharacteristic.name",
			"Service.serviceRelationship.service.serviceCharacteristic.characteristicRelationship.id",
			"Service.serviceRelationship.service.serviceCharacteristic.characteristicRelationship.relationshipType",
			"Service.serviceRelationship.service.serviceCharacteristic.characteristicRelationship.@type",
			"Service.serviceRelationship.service.serviceOrderItem.@type",
			"Service.serviceRelationship.service.serviceSpecification",
			"Service.serviceRelationship.service.serviceSpecification.id",
			"Service.serviceRelationship.service.serviceSpecification.@type",
			"Service.serviceRelationship.service.supportingResource.id",
			"Service.serviceRelationship.service.supportingResource.@type",
			"Service.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.id",
			"Service.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.relationshipType",
			"Service.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.@type",
			"Service.supportingResource.id",
			"Service.supportingResource.@type",
			"Service.supportingService.@type",
			"Service.supportingService.id",
			"Service.supportingService.state",
			"Service.supportingService.externalIdentifier.id",
			"Service.supportingService.externalIdentifier.@type",
			"Service.supportingService.feature.@type",
			"Service.supportingService.feature.name",
			"Service.supportingService.feature.featureCharacteristic.@type",
			"Service.supportingService.feature.featureCharacteristic.name",
			"Service.supportingService.feature.featureCharacteristic.characteristicRelationship.id",
			"Service.supportingService.feature.featureCharacteristic.characteristicRelationship.relationshipType",
			"Service.supportingService.feature.featureCharacteristic.characteristicRelationship.@type",
			"Service.supportingService.feature.featureRelationship.id",
			"Service.supportingService.feature.featureRelationship.relationshipType",
			"Service.supportingService.feature.featureRelationship.@type",
			"Service.supportingService.feature.policyConstraint.id",
			"Service.supportingService.feature.policyConstraint.@type",
			"Service.supportingService.intent.@type",
			"Service.supportingService.intent.id",
			"Service.supportingService.intent.name",
			"Service.supportingService.intent.attachment.attachmentType",
			"Service.supportingService.intent.attachment.id",
			"Service.supportingService.intent.attachment.mimeType",
			"Service.supportingService.intent.attachment.@type",
			"Service.supportingService.intent.characteristic.@type",
			"Service.supportingService.intent.characteristic.name",
			"Service.supportingService.intent.characteristic.characteristicRelationship.id",
			"Service.supportingService.intent.characteristic.characteristicRelationship.relationshipType",
			"Service.supportingService.intent.characteristic.characteristicRelationship.@type",
			"Service.supportingService.intent.expression",
			"Service.supportingService.intent.expression.expressionValue",
			"Service.supportingService.intent.expression.@type",
			"Service.supportingService.intent.intentRelationship.@referredType",
			"Service.supportingService.intent.intentRelationship.@type",
			"Service.supportingService.intent.intentRelationship.id",
			"Service.supportingService.intent.intentRelationship.relationshipType",
			"Service.supportingService.intent.intentRelationship.associationSpec.id",
			"Service.supportingService.intent.intentRelationship.associationSpec.@type",
			"Service.supportingService.intent.intentSpecification.id",
			"Service.supportingService.intent.intentSpecification.@type",
			"Service.supportingService.intent.relatedParty.@type",
			"Service.supportingService.intent.relatedParty.role",
			"Service.supportingService.intent.relatedParty.partyOrPartyRole.id",
			"Service.supportingService.intent.relatedParty.partyOrPartyRole.@type",
			"Service.supportingService.note.@type",
			"Service.supportingService.place.@type",
			"Service.supportingService.place.role",
			"Service.supportingService.place.place",
			"Service.supportingService.place.place.@type",
			"Service.supportingService.place.place.id",
			"Service.supportingService.place.place.calendar.@type",
			"Service.supportingService.place.place.calendar.status",
			"Service.supportingService.place.place.calendar.hourPeriod.@type",
			"Service.supportingService.place.place.countryCode.href",
			"Service.supportingService.place.place.countryCode.@type",
			"Service.supportingService.place.place.externalIdentifier.id",
			"Service.supportingService.place.place.externalIdentifier.@type",
			"Service.supportingService.place.place.geographicLocation.id",
			"Service.supportingService.place.place.geographicLocation.@type",
			"Service.supportingService.place.place.geographicSubAddress.@type",
			"Service.supportingService.place.place.geographicSubAddress.subUnit.subUnitNumber",
			"Service.supportingService.place.place.geographicSubAddress.subUnit.subUnitType",
			"Service.supportingService.place.place.geographicSubAddress.subUnit.@type",
			"Service.supportingService.place.place.relatedParty.@type",
			"Service.supportingService.place.place.relatedParty.role",
			"Service.supportingService.place.place.relatedParty.partyOrPartyRole.id",
			"Service.supportingService.place.place.relatedParty.partyOrPartyRole.@type",
			"Service.supportingService.place.place.siteRelationship.id",
			"Service.supportingService.place.place.siteRelationship.relationshipType",
			"Service.supportingService.place.place.siteRelationship.@type",
			"Service.supportingService.relatedEntity.@type",
			"Service.supportingService.relatedEntity.role",
			"Service.supportingService.relatedEntity.entity",
			"Service.supportingService.relatedEntity.entity.id",
			"Service.supportingService.relatedEntity.entity.@type",
			"Service.supportingService.relatedParty.@type",
			"Service.supportingService.relatedParty.role",
			"Service.supportingService.relatedParty.partyOrPartyRole.id",
			"Service.supportingService.relatedParty.partyOrPartyRole.@type",
			"Service.supportingService.serviceCharacteristic.@type",
			"Service.supportingService.serviceCharacteristic.name",
			"Service.supportingService.serviceCharacteristic.characteristicRelationship.id",
			"Service.supportingService.serviceCharacteristic.characteristicRelationship.relationshipType",
			"Service.supportingService.serviceCharacteristic.characteristicRelationship.@type",
			"Service.supportingService.serviceOrderItem.@type",
			"Service.supportingService.serviceRelationship.@type",
			"Service.supportingService.serviceRelationship.relationshipType",
			"Service.supportingService.serviceRelationship.serviceRelationshipCharacteristic.@type",
			"Service.supportingService.serviceRelationship.serviceRelationshipCharacteristic.name",
			"Service.supportingService.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.id",
			"Service.supportingService.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.relationshipType",
			"Service.supportingService.serviceRelationship.serviceRelationshipCharacteristic.characteristicRelationship.@type",
			"Service.supportingService.serviceSpecification",
			"Service.supportingService.serviceSpecification.id",
			"Service.supportingService.serviceSpecification.@type",
			"Service.supportingService.supportingResource.id",
			"Service.supportingService.supportingResource.@type"
	);
	
	@Test
	public void test() {

		List<String> input = new LinkedList<>(labels);
		
		List<String> res = Sorter.sortedTreeView(input);
		
		// Out.debug("TreeViewSor:: input len={} res len={}", labels.size(), res.size());
		
		assert( input.size() == res.size() );
		
		Iterator<String> iter_res = res.iterator();
		Iterator<String> iter_expected = expected_result.iterator();
		
		while(iter_res.hasNext() && iter_expected.hasNext()) {
			assert( iter_res.next() == iter_expected.next() );
		}
		
	}

}
