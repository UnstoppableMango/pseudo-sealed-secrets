{
  jre,
  lib,
  makeWrapper,
  maven,
}:
let
  version = "0.1.0";
  jarFile = "pseudo-sealed-secrets-${version}-SNAPSHOT.jar";
in
maven.buildMavenPackage {
  pname = "pseudo-sealed-secrets";
  inherit version;

  src = lib.cleanSource ../.;
  mvnHash = "sha256-WONgvKVO1tSocaQIeqTA/uzWm4Sx5y0fo/worAavsjo=";

  nativeBuildInputs = [ makeWrapper ];

  doCheck = false;

  installPhase = ''
    mkdir -p $out/bin $out/share/pseudo-sealed-secrets
    install -Dm644 target/${jarFile} $out/share/pseudo-sealed-secrets

    makeWrapper ${jre}/bin/java $out/bin/pseudo-sealed-secrets \
      --add-flags "-jar $out/share/pseudo-sealed-secrets/${jarFile}"
  '';
}
