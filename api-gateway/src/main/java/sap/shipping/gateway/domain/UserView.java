package sap.shipping.gateway.domain;

/**
 * The gateway's view of a registered/authenticated user.
 */
public record UserView(
    String userId,
    String username
) {}
