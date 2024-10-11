package org.knowm.xchange.ripple;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.ripple.dto.RippleException;
import org.knowm.xchange.ripple.dto.trade.RippleOrderCancelRequest;
import org.knowm.xchange.ripple.dto.trade.RippleOrderCancelResponse;
import org.knowm.xchange.ripple.dto.trade.RippleOrderEntryRequest;
import org.knowm.xchange.ripple.dto.trade.RippleOrderEntryResponse;

import java.io.IOException;

/** See https://github.com/ripple/ripple-rest for up-to-date documentation. */
@Path("v1")
@Produces(MediaType.APPLICATION_JSON)
public interface RippleAuthenticated {

  /** Places an order */
  @POST
  @Path("accounts/{address}/orders")
  @Consumes(MediaType.APPLICATION_JSON)
  RippleOrderEntryResponse orderEntry(
      @PathParam("address") final String address,
      @QueryParam("validated") final boolean validated,
      final RippleOrderEntryRequest request)
      throws IOException, RippleException;

  /** Cancel an order */
  @DELETE
  @Path("accounts/{address}/orders/{orderId}")
  @Consumes(MediaType.APPLICATION_JSON)
  RippleOrderCancelResponse orderCancel(
      @PathParam("address") final String address,
      @PathParam("orderId") final long orderId,
      @QueryParam("validated") final boolean validated,
      final RippleOrderCancelRequest request)
      throws IOException, RippleException;
}
