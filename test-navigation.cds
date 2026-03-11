using CommonConfigurationService from './CommonConfigurationService';

annotate CommonConfigurationService.S4Mapping with
@odata.draft.enabled;

annotate CommonConfigurationService.S4IntegrationConfiguration with
@odata.draft.enabled;

annotate CommonConfigurationService.S4MappingForTile with
@odata.singleton;

annotate CommonConfigurationService.S4ConfigToReplicate with
@Capabilities: {
    SortRestrictions  : {
        $Type                : 'Capabilities.SortRestrictionsType',
        NonSortableProperties: [
            id, syncStatusCode, defaultSortOrder
        ]
    }
}{
    displayId
    @title : '{i18n>ObjectId}';

    name
    @title : '{i18n>ObjectName}';

    syncStatusName
    @title : '{i18n>SyncStatus}';

    objectTypeName
    @title : '{i18n>ObjectType}';
};

annotate CommonConfigurationService.S4PropertyTree with {
    propertyTreeId
    @title: '{i18n>S4PropertyTreeId}';

    description
    @title: '{i18n>S4PropertyTreeName}';
}
