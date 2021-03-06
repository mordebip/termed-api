package fi.thl.termed.util;

import java.util.UUID;

public final class URIs {

  private URIs() {
  }

  public static String localName(String uri) {
    int i = uri.lastIndexOf("#");
    i = i == -1 ? uri.lastIndexOf("/") : i;
    i = i == -1 ? uri.lastIndexOf(":") : i;
    return uri.substring(i + 1);
  }

  public static String urnUuid(UUID uuid) {
    return String.format("urn:uuid:%s", uuid);
  }

  public static String ensureTrailingSlashOrHash(String ns) {
    return ns.isEmpty() || ns.endsWith("/") || ns.endsWith("#") ? ns : ns + "/";
  }

}
