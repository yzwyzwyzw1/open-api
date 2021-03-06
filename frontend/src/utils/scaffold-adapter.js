export const adaptScaffold = fields => {
  let adapted = { ...fields };
  if (fields.description) {
    adapted = { ...adapted, title: fields.description };
  }
  if (fields.vendorAddress) {
    adapted = { ...adapted, developerAddress: fields.vendorAddress };
  }
  return adapted;
};

export const serializeScaffold = fields => {
  let serialized = { ...fields };
  if (fields.title) {
    serialized = { ...serialized, description: fields.title };
  }
  if (fields.developerAddress) {
    serialized = { ...serialized, vendorAddress: fields.developerAddress };
  }
  return serialized;
};
