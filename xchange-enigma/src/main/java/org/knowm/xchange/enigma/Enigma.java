package org.knowm.xchange.enigma;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.knowm.xchange.enigma.dto.account.EnigmaCredentials;
import org.knowm.xchange.enigma.dto.account.EnigmaLoginResponse;

import java.io.IOException;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public interface Enigma {

  @POST
  @Path("user/login")
  @Consumes(MediaType.APPLICATION_JSON)
  EnigmaLoginResponse login(EnigmaCredentials loginRequest) throws IOException;
}
