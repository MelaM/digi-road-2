(function(root) {
  root.AssetTypeConfiguration = function () {

    var assetType = {
      totalWeightLimit: 30,
      trailerTruckWeightLimit: 40,
      axleWeightLimit: 50,
      bogieWeightLimit: 60,
      heightLimit: 70,
      lengthLimit: 80,
      widthLimit: 90,
      litRoad: 100,
      pavedRoad: 110,
      width: 120,
      damagedByThaw: 130,
      numberOfLanes: 140,
      congestionTendency: 150,
      massTransitLane: 160,
      trafficVolume: 170,
      winterSpeedLimit: 180,
      prohibition: 190,
      pedestrianCrossings: 200,
      hazardousMaterialTransportProhibition: 210,
      obstacles: 220,
      railwayCrossings: 230,
      directionalTrafficSigns: 240,
      servicePoints: 250,
      europeanRoads: 260,
      exitNumbers: 270,
      trafficLights: 280,
      maintenanceRoad: 290,
      trafficSigns: 300,
      trSpeedLimits: 310,
      trWeightLimits: 320,
      trTrailerTruckWeightLimits: 330,
      trAxleWeightLimits: 340,
      trBogieWeightLimits: 350,
      trHeightLimits: 360,
      trWidthLimits: 370,
      carryingCapacity: 400
    };

    var assetGroups = {
      trWeightGroup: [assetType.trWeightLimits, assetType.trTrailerTruckWeightLimits, assetType.trAxleWeightLimits, assetType.trBogieWeightLimits]
    };

    var linearAssetSpecs = [
      {
        typeId: assetType.totalWeightLimit,
        singleElementEventCategory: 'totalWeightLimit',
        multiElementEventCategory: 'totalWeightLimits',
        layerName: 'totalWeightLimit',
        title: 'Suurin sallittu massa',
        newTitle: 'Uusi suurin sallittu massa',
        className: 'total-weight-limit',
        unit: 'kg',
        isSeparable: false,
        allowComplementaryLinks: true,
        editControlLabels: {
          title: 'Rajoitus',
          enabled: 'Rajoitus',
          disabled: 'Ei rajoitusta',
          massLimitations : 'Muut massarajoitukset',
          showUnit: true
        },
        label: new MassLimitationsLabel(),
        hasTrafficSignReadOnlyLayer: true,
        isVerifiable: true,
        hasMunicipalityValidation: true,
        authorizationPolicy: new LinearAssetAuthorizationPolicy(),
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.trailerTruckWeightLimit,
        singleElementEventCategory: 'trailerTruckWeightLimit',
        multiElementEventCategory: 'trailerTruckWeightLimits',
        layerName: 'trailerTruckWeightLimit',
        title: 'Yhdistelmän suurin sallittu massa',
        newTitle: 'Uusi yhdistelmän suurin sallittu massa',
        className: 'trailer-truck-weight-limit',
        unit: 'kg',
        isSeparable: false,
        allowComplementaryLinks: true,
        editControlLabels: { title: 'Rajoitus',
          enabled: 'Rajoitus',
          disabled: 'Ei rajoitusta',
          massLimitations : 'Muut massarajoitukset',
          showUnit: true
        },
        label: new MassLimitationsLabel(),
        hasTrafficSignReadOnlyLayer: true,
        isVerifiable: true,
        hasMunicipalityValidation: true,
        authorizationPolicy: new LinearAssetAuthorizationPolicy(),
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.axleWeightLimit,
        singleElementEventCategory: 'axleWeightLimit',
        multiElementEventCategory: 'axleWeightLimits',
        layerName: 'axleWeightLimit',
        title: 'Suurin sallittu akselimassa',
        newTitle: 'Uusi suurin sallittu akselimassa',
        className: 'axle-weight-limit',
        unit: 'kg',
        isSeparable: false,
        allowComplementaryLinks: true,
        editControlLabels: { title: 'Rajoitus',
          enabled: 'Rajoitus',
          disabled: 'Ei rajoitusta',
          massLimitations : 'Muut massarajoitukset',
          showUnit: true
        },
        label: new MassLimitationsLabel(),
        hasTrafficSignReadOnlyLayer: true,
        isVerifiable: true,
        hasMunicipalityValidation: true,
        authorizationPolicy: new LinearAssetAuthorizationPolicy(),
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.bogieWeightLimit,
        singleElementEventCategory: 'bogieWeightLimit',
        multiElementEventCategory: 'bogieWeightlLimits',
        layerName: 'bogieWeightLimit',
        title: 'Suurin sallittu telimassa',
        newTitle: 'Uusi suurin sallittu telimassa',
        className: 'bogie-weight-limit',
        unit: 'kg',
        isSeparable: false,
        allowComplementaryLinks: true,
        editControlLabels: { title: 'Rajoitus',
          enabled: 'Rajoitus',
          disabled: 'Ei rajoitusta',
          massLimitations : 'Muut massarajoitukset',
          showUnit: true
        },
        label: new MassLimitationsLabel(),
        hasTrafficSignReadOnlyLayer: true,
        isVerifiable: true,
        hasMunicipalityValidation: true,
        authorizationPolicy: new LinearAssetAuthorizationPolicy(),
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.heightLimit,
        singleElementEventCategory: 'heightLimit',
        multiElementEventCategory: 'heightLimits',
        layerName: 'heightLimit',
        title: 'Suurin sallittu korkeus',
        newTitle: 'Uusi suurin sallittu korkeus',
        className: 'height-limit',
        unit: 'cm',
        isSeparable: false,
        allowComplementaryLinks: true,
        editControlLabels: { title: 'Rajoitus',
          enabled: 'Rajoitus',
          disabled: 'Ei rajoitusta',
          showUnit: true
        },
        label: new LinearAssetLabel(),
        hasTrafficSignReadOnlyLayer: true,
        isVerifiable: true,
        hasMunicipalityValidation: true,
        isMultipleLinkSelectionAllowed: true,
        authorizationPolicy: new LinearAssetAuthorizationPolicy()
      },
      {
        typeId: assetType.lengthLimit,
        singleElementEventCategory: 'lengthLimit',
        multiElementEventCategory: 'lengthLimits',
        layerName: 'lengthLimit',
        title: 'Suurin sallittu pituus',
        newTitle: 'Uusi pituusrajoitus',
        className: 'length-limit',
        unit: 'cm',
        isSeparable: false,
        allowComplementaryLinks: true,
        editControlLabels: { title: 'Rajoitus',
          enabled: 'Rajoitus',
          disabled: 'Ei rajoitusta',
          showUnit: true
        },
        label: new LinearAssetLabel(),
        hasTrafficSignReadOnlyLayer: true,
        isVerifiable: true,
        hasMunicipalityValidation: true,
        isMultipleLinkSelectionAllowed: true,
        authorizationPolicy: new LinearAssetAuthorizationPolicy()
      },
      {
        typeId: assetType.widthLimit,
        singleElementEventCategory: 'widthLimit',
        multiElementEventCategory: 'widthLimits',
        layerName: 'widthLimit',
        title: 'Suurin sallittu leveys',
        newTitle: 'Uusi suurin sallittu leveys',
        className: 'width-limit',
        unit: 'cm',
        isSeparable: false,
        allowComplementaryLinks: true,
        editControlLabels: {
          title: 'Rajoitus',
          enabled: 'Rajoitus',
          disabled: 'Ei rajoitusta',
          showUnit: true
        },
        label: new LinearAssetLabel(),
        hasTrafficSignReadOnlyLayer: true,
        isVerifiable: true,
        hasMunicipalityValidation: true,
        isMultipleLinkSelectionAllowed: true,
        authorizationPolicy: new LinearAssetAuthorizationPolicy()
      },
      {
        typeId: assetType.litRoad,
        defaultValue: 1,
        singleElementEventCategory: 'litRoad',
        multiElementEventCategory: 'litRoads',
        layerName: 'litRoad',
        title: 'Valaistus',
        newTitle: 'Uusi valaistus',
        className: 'lit-road',
        isSeparable: false,
        allowComplementaryLinks: true,
        editControlLabels: {
          title: 'Valaistus',
          enabled: 'Valaistus',
          disabled: 'Ei valaistusta'
        },
        authorizationPolicy: new LinearStateRoadAuthorizationPolicy(),
        isVerifiable: true,
        hasMunicipalityValidation: true,
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.damagedByThaw,
        defaultValue: 1,
        singleElementEventCategory: 'roadDamagedByThaw',
        multiElementEventCategory: 'roadsDamagedByThaw',
        layerName: 'roadDamagedByThaw',
        title: 'Kelirikko',
        newTitle: 'Uusi kelirikko',
        className: 'road-damaged-by-thaw',
        isSeparable: false,
        allowComplementaryLinks: true,
        editControlLabels: {
          title: 'Kelirikko',
          enabled: 'Kelirikko',
          disabled: 'Ei kelirikkoa'
        },
        authorizationPolicy: new LinearStateRoadAuthorizationPolicy(),
        isVerifiable: false,
        label: new RoadDamagedByThawLabel(),
        form: new DynamicAssetForm ( {
          fields : [
            { publicId: 'kelirikko',  label:'rajoitus', type: 'number', weigth: 1, unit: 'kg' }
          ]
        }),
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.width,
        singleElementEventCategory: 'roadWidth',
        multiElementEventCategory: 'roadWidth',
        layerName: 'roadWidth',
        title: 'Leveys',
        newTitle: 'Uusi leveys',
        className: 'road-width',
        unit: 'cm',
        isSeparable: false,
        allowComplementaryLinks: true,
        editControlLabels: {
          title: 'Leveys',
          enabled: 'Leveys tiedossa',
          disabled: 'Leveys ei tiedossa',
          showUnit: true
        },
        label: new LinearAssetLabel(),
        authorizationPolicy: new LinearStateRoadAuthorizationPolicy(),
        isVerifiable: true,
        hasMunicipalityValidation: true,
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.congestionTendency,
        defaultValue: 1,
        singleElementEventCategory: 'congestionTendency',
        multiElementEventCategory: 'congestionTendencies',
        layerName: 'congestionTendency',
        title: 'Ruuhkaantumisherkkyys',
        newTitle: 'Uusi ruuhkautumisherkkä tie',
        className: 'congestion-tendency',
        isSeparable: false,
        allowComplementaryLinks: false,
        editControlLabels: {
          title: 'Herkkyys',
          enabled: 'Ruuhkaantumisherkkä',
          disabled: 'Ei ruuhkaantumisherkkä'
        },
        isVerifiable: false
      },
      {
        typeId: assetType.pavedRoad,
        defaultValue: 1,
        singleElementEventCategory: 'pavedRoad',
        multiElementEventCategory: 'pavedRoads',
        layerName: 'pavedRoad',
        title: 'Päällyste',
        newTitle: 'Uusi päällyste',
        className: 'paved-road',
        isSeparable: false,
        allowComplementaryLinks: true,
        editControlLabels: {
          title: 'Päällyste',
          enabled: 'Päällyste',
          disabled: 'Ei päällystettä'
        },
        authorizationPolicy: new LinearStateRoadAuthorizationPolicy(),
        isVerifiable: false,
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.trafficVolume,
        singleElementEventCategory: 'trafficVolume',
        multiElementEventCategory: 'trafficVolumes',
        layerName: 'trafficVolume',
        title: 'Liikennemäärä',
        newTitle: 'Uusi liikennemäärä',
        className: 'traffic-volume',
        unit: 'ajoneuvoa/vuorokausi',
        isSeparable: false,
        allowComplementaryLinks: false,
        editControlLabels: {
          title: '',
          enabled: 'Liikennemäärä',
          disabled: 'Ei tiedossa',
          showUnit: true
        },
        label: new LinearAssetLabel(),
        authorizationPolicy: new ReadOnlyAuthorizationPolicy(),
        isVerifiable: true
      },
      {
        typeId: assetType.massTransitLane,
        defaultValue: 1,
        singleElementEventCategory: 'massTransitLane',
        multiElementEventCategory: 'massTransitLanes',
        layerName: 'massTransitLanes',
        title: 'Joukkoliikennekaista',
        newTitle: 'Uusi joukkoliikennekaista',
        className: 'mass-transit-lane',
        isSeparable: true,
        allowComplementaryLinks: true,
        editControlLabels: {
          title: 'Kaista',
          enabled: 'Joukkoliikennekaista',
          disabled: 'Ei joukkoliikennekaistaa'
        },
        authorizationPolicy: new LinearStateRoadAuthorizationPolicy(),
        isVerifiable: true,
        isMultipleLinkSelectionAllowed: true,
        form: new DynamicAssetForm({
          fields: [
            {label: "", type: 'time_period', publicId: "public_validity_period", weight: 1}
          ]
        })
      },
      {
        typeId: assetType.winterSpeedLimit,
        singleElementEventCategory: 'winterSpeedLimit',
        multiElementEventCategory: 'winterSpeedLimits',
        layerName: 'winterSpeedLimits',
        title: 'Talvinopeusrajoitus',
        newTitle: 'Uusi talvinopeusrajoitus',
        className: 'winter-speed-limits',
        unit: 'km/h',
        isSeparable: true,
        allowComplementaryLinks: true,
        editControlLabels: {
          title: 'Rajoitus',
          enabled: 'Talvinopeusrajoitus',
          disabled: 'Ei talvinopeusrajoitusta',
          showUnit: true
        },
        possibleValues: [100, 80, 70, 60],
        style : new WinterSpeedLimitStyle(),
        isVerifiable: false,
        isMultipleLinkSelectionAllowed: true,
        authorizationPolicy: new LinearAssetAuthorizationPolicy()
      },
      {
        typeId: assetType.prohibition,
        singleElementEventCategory: 'prohibition',
        multiElementEventCategory: 'prohibitions',
        layerName: 'prohibition',
        title: 'Ajoneuvokohtaiset rajoitukset',
        newTitle: 'Uusi ajoneuvokohtainen rajoitus',
        className: 'prohibition',
        isSeparable: true,
        allowComplementaryLinks: true,
        editControlLabels: {
          title: 'Rajoitus',
          enabled: 'Rajoitus',
          disabled: 'Ei rajoitusta'
        },
        isVerifiable: true,
        isMultipleLinkSelectionAllowed: true,
        authorizationPolicy: new LinearAssetAuthorizationPolicy()
      },
      {
        typeId: assetType.hazardousMaterialTransportProhibition,
        singleElementEventCategory: 'hazardousMaterialTransportProhibition',
        multiElementEventCategory: 'hazardousMaterialTransportProhibitions',
        layerName: 'hazardousMaterialTransportProhibition',
        title: 'VAK-rajoitus',
        newTitle: 'Uusi VAK-rajoitus',
        className: 'hazardousMaterialTransportProhibition',
        isSeparable: true,
        allowComplementaryLinks: true,
        editControlLabels: {
          title: 'VAK-rajoitus',
          enabled: 'Rajoitus',
          disabled: 'Ei rajoitusta'
        },
        isVerifiable: true,
        isMultipleLinkSelectionAllowed: true,
        authorizationPolicy: new LinearAssetAuthorizationPolicy()
      },
      {
        typeId: assetType.europeanRoads,
        singleElementEventCategory: 'europeanRoad',
        multiElementEventCategory: 'europeanRoads',
        layerName: 'europeanRoads',
        title: 'Eurooppatienumero',
        newTitle: 'Uusi eurooppatienumero',
        className: 'european-road',
        unit: '',
        isSeparable: false,
        allowComplementaryLinks: false,
        editControlLabels: {
          title: '',
          enabled: 'Eurooppatienumero(t)',
          disabled: 'Ei eurooppatienumeroa'
        },
        authorizationPolicy: new LinearStateRoadAuthorizationPolicy(),
        label: new LinearAssetLabelMultiValues(),
        isVerifiable: false,
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.exitNumbers,
        singleElementEventCategory: 'exitNumber',
        multiElementEventCategory: 'exitNumbers',
        layerName: 'exitNumbers',
        title: 'Liittymänumero',
        newTitle: 'Uusi liittymänumero',
        className: 'exit-number',
        unit: '',
        isSeparable: false,
        allowComplementaryLinks: false,
        editControlLabels: {
          title: '',
          enabled: 'Liittymänumero(t)',
          disabled: 'Ei liittymänumeroa'
        },
        label: new LinearAssetLabelMultiValues(),
        isVerifiable: false,
        authorizationPolicy: new LinearAssetAuthorizationPolicy(),
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.maintenanceRoad,
        singleElementEventCategory: 'maintenanceRoad',
        multiElementEventCategory: 'maintenanceRoads',
        layerName: 'maintenanceRoad',
        title: 'Rautateiden huoltotie',
        newTitle: 'Uusi rautateiden huoltotie',
        className: 'maintenanceRoad',
        isSeparable: false,
        unit: '',
        allowComplementaryLinks: true,
        editControlLabels: {
          title: '',
          enabled: 'Huoltotie',
          disabled: 'Ei huoltotietä'
        },
        possibleValues: [
          {'name': 'Käyttöoikeus', 'propType': 'single_choice', 'id': "huoltotie_kayttooikeus",
                  value: [
                          {typeId: 1, title: 'Tieoikeus'},
                          {typeId: 2, title: 'Tiekunnan osakkuus'},
                          {typeId: 3, title: 'LiVin hallinnoimalla maa-alueella'},
                          {typeId: 4, title: 'Kevyen liikenteen väylä'},
                          {typeId: 6, title: 'Muu sopimus'},
                          {typeId: 9, title: 'Potentiaalinen käyttöoikeus'},
                          {typeId: 99, title: 'Tuntematon'}
                          ]},
          {'name': 'Huoltovastuu', 'propType': 'single_choice', 'id': "huoltotie_huoltovastuu", value: [{typeId: 1, title: 'LiVi'}, {typeId: 2, title: 'Muu'}, {typeId: 99, title: 'Ei tietoa'}]},
          {'name': "Tiehoitokunta", 'propType': 'text', 'id': "huoltotie_tiehoitokunta" },
          {'name': "Yhteyshenkilö", 'propType': 'header' },
          {'name': "Nimi", 'propType': 'text', 'id': "huoltotie_nimi" },
          {'name': "Osoite", 'propType': 'text', 'id': "huoltotie_osoite"},
          {'name': "Postinumero", 'propType': 'text', 'id': "huoltotie_postinumero"},
          {'name': "Postitoimipaikka", 'propType': 'text', 'id': "huoltotie_postitoimipaikka"},
          {'name': "Puhelin 1", 'propType': 'text', 'id': "huoltotie_puh1"},
          {'name': "Puhelin 2", 'propType': 'text', 'id': "huoltotie_puh2"},
          {'name': "Lisätietoa", 'propType': 'text', 'id': "huoltotie_lisatieto"},
          {'name': "Tarkistettu", 'propType': 'checkbox', 'id': "huoltotie_tarkistettu", value: [{typeId: 0, title: 'Ei tarkistettu'}, {typeId: 1, title: 'Tarkistettu'}]}],
        style: new ServiceRoadStyle(),
        label : new ServiceRoadLabel(),
        isVerifiable: true,
        layer : ServiceRoadLayer,
        collection: ServiceRoadCollection,
        authorizationPolicy: new ServiceRoadAuthorizationPolicy(),
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.numberOfLanes,
        singleElementEventCategory: 'laneCount',
        multiElementEventCategory: 'laneCounts',
        layerName: 'numberOfLanes',
        title: 'Kaistojen lukumäärä',
        newTitle: 'Uusi kaistojen lukumäärä',
        className: 'lane-count',
        unit: 'kpl / suunta',
        isSeparable: true,
        allowComplementaryLinks: true,
        editControlLabels: {
          title: 'Lukumäärä',
          enabled: 'Kaistojen lukumäärä / suunta',
          disabled: 'Ei tietoa'
        },
        label: new LinearAssetLabel(),
        isVerifiable: true,
        authorizationPolicy: new LinearAssetAuthorizationPolicy(),
        isMultipleLinkSelectionAllowed: true
      },
      {
        typeId: assetType.carryingCapacity,
        singleElementEventCategory: 'carryingCapacity',
        multiElementEventCategory: 'carryingCapacity',
        layerName: 'carryingCapacity',
        title: 'Kantavuus',
        newTitle: 'Uusi Kantavuus',
        className: 'carrying-capacity',
        unit: '',
        isSeparable: false,
        allowComplementaryLinks: false,
        editControlLabels: {
          title: 'Kantavuus',
          enabled: 'Kantavuus',
          disabled: 'Ei Kantavuutta'
        },
        label: new LinearAssetLabel(),
        authorizationPolicy: new LinearStateRoadAuthorizationPolicy(),
        isVerifiable: false,
        form: new DynamicAssetForm({
          fields: [
            {label: "KEVÄTKANTAVUUS", type: 'integer', publicId: "integer_publicID", unit: "MN/m<sup>2</sup>", weight: 1},
            {label: "ROUTIVUUSKERROIN", type: 'single_choice', publicId: "single_choice_publicID",
                values: [{id: 1, label: "40 Erittäin routiva"},
                         {id: 2, label: "50 Väliarvo 50...60"},
                         {id: 3, label: "60 Routiva"},
                         {id: 4, label: "70 Väliarvo 60...80"},
                         {id: 5, label: "80 Routimaton"},
                         {id: 99, label: 'Ei tietoa'}], weight: 2, defaultValue: "99"},
            {label: "MITTAUSPÄIVÄ", type: 'date', publicId: "date_publicID", weight: 3}
          ]
        })
      }
    ];

    var experimentalLinearAssetSpecs = [
      {
        typeId: assetType.trSpeedLimits,
        singleElementEventCategory: 'trSpeedLimit',
        multiElementEventCategory: 'trSpeedLimits',
        layerName: 'trSpeedLimits',
        title: 'Tierekisteri nopeusrajoitus',
        newTitle: 'Uusi nopeusrajoitus',
        className: 'tr-speed-limits',
        unit: 'km/h',
        isSeparable: true,
        allowComplementaryLinks: false,
        editControlLabels: {
          title: '',
          enabled: 'Nopeusrajoitus',
          disabled: 'Tuntematon'
        },
        label: new TRSpeedLimitAssetLabel(),
        hasTrafficSignReadOnlyLayer: true,
        style: new TRSpeedLimitStyle(),
        authorizationPolicy: new ReadOnlyAuthorizationPolicy()
      }
    ];

    var pointAssetSpecs = [
      {
        typeId: assetType.pedestrianCrossings,
        layerName: 'pedestrianCrossings',
        title: 'Suojatie',
        allowComplementaryLinks: true,
        newAsset: {  },
        legendValues: [
          {symbolUrl: 'images/point-assets/point_blue.svg', label: 'Suojatie'},
          {symbolUrl: 'images/point-assets/point_red.svg', label: 'Geometrian ulkopuolella'}
        ],
        formLabels: {
          singleFloatingAssetLabel: 'suojatien',
          manyFloatingAssetsLabel: 'suojatiet',
          newAssetLabel: 'suojatie'
        },
        hasMunicipalityValidation: true,
        authorizationPolicy: new PointAssetAuthorizationPolicy()
      },
      {
        typeId: assetType.obstacles,
        layerName: 'obstacles',
        title: 'Esterakennelma',
        allowComplementaryLinks: true,
        newAsset: { obstacleType: 1 },
        legendValues: [
          {symbolUrl: 'images/point-assets/point_blue.svg', label: 'Suljettu yhteys'},
          {symbolUrl: 'images/point-assets/point_green.svg', label: 'Avattava puomi'},
          {symbolUrl: 'images/point-assets/point_red.svg', label: 'Geometrian ulkopuolella'}
        ],
        formLabels: {
          singleFloatingAssetLabel: 'esterakennelman',
          manyFloatingAssetsLabel: 'esterakennelmat',
          newAssetLabel: 'esterakennelma'
        },
        authorizationPolicy: new PointAssetAuthorizationPolicy()
      },
      {
        typeId: assetType.railwayCrossings,
        layerName: 'railwayCrossings',
        title: 'Rautatien tasoristeys',
        allowComplementaryLinks: true,
        newAsset: { safetyEquipment: 1 },
        legendValues: [
          {symbolUrl: 'images/point-assets/point_blue.svg', label: 'Rautatien tasoristeys'},
          {symbolUrl: 'images/point-assets/point_red.svg', label: 'Geometrian ulkopuolella'}
        ],
        formLabels: {
          singleFloatingAssetLabel: 'tasoristeyksen',
          manyFloatingAssetsLabel: 'tasoristeykset',
          newAssetLabel: 'tasoristeys'
        },
        saveCondition: function(selectedAsset) {
            var selected = selectedAsset .get();
          return selected.code ? selected.code !== '' : false;
        },
        authorizationPolicy: new PointAssetAuthorizationPolicy()
      },
      {
        typeId: assetType.directionalTrafficSigns,
        layerName: 'directionalTrafficSigns',
        title: 'Opastustaulu',
        allowComplementaryLinks: false,
        newAsset: { validityDirection: 2 },
        legendValues: [
          {symbolUrl: 'src/resources/digiroad2/bundle/assetlayer/images/direction-arrow-directional-traffic-sign.svg', label: 'Opastustaulu'},
          {symbolUrl: 'src/resources/digiroad2/bundle/assetlayer/images/direction-arrow-warning-directional-traffic-sign.svg', label: 'Geometrian ulkopuolella'}
        ],
        formLabels: {
          singleFloatingAssetLabel: 'opastustaulun',
          manyFloatingAssetsLabel: 'opastustaulut',
          newAssetLabel: 'opastustaulu'
        },
        authorizationPolicy: new PointAssetAuthorizationPolicy()
      },
      {
        typeId: assetType.servicePoints,
        layerName: 'servicePoints',
        title: 'Palvelupiste',
        allowComplementaryLinks: false,
        newAsset: { services: [] },
        legendValues: [
          {symbolUrl: 'images/point-assets/point_blue.svg', label: 'Palvelupiste'}
        ],
        formLabels: {
          singleFloatingAssetLabel: 'palvelupisteen',
          manyFloatingAssetsLabel: 'palvelupisteet',
          newAssetLabel: 'palvelupiste'
        },
        authorizationPolicy: new ServicePointAuthorizationPolicy()
      },
      {
        typeId: assetType.trafficLights,
        layerName: 'trafficLights',
        title: 'Liikennevalo',
        allowComplementaryLinks: true,
        newAsset: {  },
        legendValues: [
          {symbolUrl: 'images/point-assets/point_blue.svg', label: 'Liikennevalo'},
          {symbolUrl: 'images/point-assets/point_red.svg', label: 'Geometrian ulkopuolella'}
        ],
        formLabels: {
          singleFloatingAssetLabel: 'liikennevalojen',
          manyFloatingAssetsLabel: 'liikennevalot',
          newAssetLabel: 'liikennevalo'
        },
        hasMunicipalityValidation: true,
        authorizationPolicy: new PointAssetAuthorizationPolicy()
      },
      {
        typeId: assetType.trafficSigns,
        layerName: 'trafficSigns',
        title: 'Liikennemerkit',
        allowComplementaryLinks: true,
        newAsset: { validityDirection: 2, propertyData: [
          {'name': 'Tyyppi', 'propertyType': 'single_choice', 'publicId': "trafficSigns_type", values: [ [ {propertyValue: 1} ] ] },
          {'name': "Arvo", 'propertyType': 'text', 'publicId': "trafficSigns_value", values: []},
          {'name': "Lisatieto", 'propertyType': 'text', 'publicId': "trafficSigns_info", values: []}
        ]},
        label: new TrafficSignLabel(Math.pow(3, 2)),
        collection: TrafficSignsCollection,
        allowGrouping: true,
        groupingDistance: Math.pow(3, 2), //geometry-calculations calculates the squared distance between two points, so give the grouping distance in meters x^2
        formLabels: {
          singleFloatingAssetLabel: 'liikennemerkin',
          manyFloatingAssetsLabel: 'liikennemerkit',
          newAssetLabel: 'liikennemerkki'
        },
        authorizationPolicy: new PointStateRoadAuthorizationPolicy(),
        hasMunicipalityValidation: true,
        saveCondition: function (selectedAsset) {
          var possibleSpeedLimitsValues = [20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120];
          var validations = [
            { types: [1, 2, 3, 4], validate: function (someValue) { return /^\d+$/.test(someValue) && _.contains(possibleSpeedLimitsValues, parseInt(someValue)); }},
            { types: [8, 30, 31, 32, 33, 34, 35], validate: function (someValue) { return /^\d+$/.test(someValue) ; }}
          ];

          var functionFn = _.find(validations, function(validation){ return _.contains(validation.types, parseInt(Property.getPropertyValue('Tyyppi', selectedAsset.get())));});
          return functionFn ?  functionFn.validate(Property.getPropertyValue('Arvo', selectedAsset.get())) : true;
        }
      },
      {
        typeId: assetType.trHeightLimits,
        layerName: 'trHeightLimits',
        title: 'TR suurin sallittu korkeus',
        allowComplementaryLinks: true,
        allowGrouping: true,
        groupingDistance: Math.pow(5, 2), //geometry-calculations calculates the squared distance between two points, so give the grouping distance in meters x^2
        legendValues: [
          {symbolUrl: 'images/point-assets/point_blue.svg', label: 'Rajoitus'},
          {symbolUrl: 'images/point-assets/point_red.svg', label: 'Geometrian ulkopuolella'}
        ],
        formLabels: {
          title: 'Rajoitus',
          showUnit: true
        },
        authorizationPolicy: new ReadOnlyAuthorizationPolicy(),
        nonModifiableBox: true,
        label: new HeightLimitLabel(Math.pow(5, 2))
      },
      {
        typeId: assetType.trWidthLimits,
        layerName: 'trWidthLimits',
        title: 'TR suurin sallittu leveys',
        allowComplementaryLinks: true,
        allowGrouping: true,
        groupingDistance: Math.pow(5, 2), //geometry-calculations calculates the squared distance between two points, so give the grouping distance in meters x^2
        legendValues: [
          {symbolUrl: 'images/point-assets/point_blue.svg', label: 'Rajoitus'},
          {symbolUrl: 'images/point-assets/point_red.svg', label: 'Geometrian ulkopuolella'}
        ],
        formLabels: {
          title: 'Rajoitus',
          showUnit: true
        },
        authorizationPolicy: new ReadOnlyAuthorizationPolicy(),
        nonModifiableBox: true,
        label: new WidthLimitLabel(Math.pow(5, 2))
      }
    ];

    var groupedPointAssetSpecs = [
      {
        typeIds: assetGroups.trWeightGroup,
        layerName: 'trWeightLimits',
        title: 'TR painorajoitukset',
        allowComplementaryLinks: true,
        allowGrouping: false,
        legendValues: [
          {symbolUrl: 'images/point-assets/point_blue.svg', label: 'Rajoitus'},
          {symbolUrl: 'images/point-assets/point_red.svg', label: 'Geometrian ulkopuolella'}
        ],
        formLabels: {
          title: 'Painorajoitus',
          showUnit: true
        },
        authorizationPolicy: new ReadOnlyAuthorizationPolicy(),
        nonModifiableBox: true,
        label: new WeightLimitLabel(),
        propertyData: [
          {'propertyTypeId': assetType.trWeightLimits, 'propertyType': 'number', 'publicId': "suurin_sallittu_massa_mittarajoitus", values: []},
          {'propertyTypeId': assetType.trTrailerTruckWeightLimits, 'propertyType': 'number', 'publicId': "yhdistelman_suurin_sallittu_massa", values: []},
          {'propertyTypeId': assetType.trAxleWeightLimits, 'propertyType': 'number', 'publicId': "suurin_sallittu_akselimassa", values: []},
          {'propertyTypeId': assetType.trBogieWeightLimits, 'propertyType': 'number', 'publicId': "suurin_sallittu_telimassa", values: []}
        ]
      }
    ];

    return {
      assetTypes : assetType,
      linearAssetsConfig : linearAssetSpecs,
      experimentalAssetsConfig : experimentalLinearAssetSpecs,
      pointAssetsConfig : pointAssetSpecs,
      groupedPointAssetSpecs: groupedPointAssetSpecs,
      assetGroups: assetGroups
    };
  };
})(this);