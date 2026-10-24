/** Actions requiring live permissions, revisions or a server-side transaction. */
export function requiresOnline(config) {
  const path = (config.url || "").split("?")[0];
  const writing = (config.method || "get").toLowerCase() !== "get";
  return (
    /^\/(charge|operation-plans|shopping-batches)(\/|$)/.test(path) ||
    /^\/(life\/(action-center|features|assignments)|reminder\/events|lifeReport\/insights|system\/dataGroup)(\/|$)/.test(
      path
    ) ||
    /^\/mealPlan\/(shopping-preview|shopping-commit|transferToShopping)$/.test(path) ||
    /^\/shopping(Item|List)\/[^/]+\/(claim|release|fulfillment|checkout|batches)$/.test(path) ||
    (writing && /^\/(bookkeeping|subscription|mealPlan|shoppingItem|shoppingList)(\/|$)/.test(path))
  );
}
