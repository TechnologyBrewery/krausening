import os

import click
from krausening import apply_encryption


# As long as we only have help/version, suppress options with blank metavar
@click.group(options_metavar="")
@click.version_option()
def krausening() -> None:
    """Command-line utility to help use Krausening."""


@krausening.group(options_metavar="")
def encrypt() -> None:
    """Encrypts secrets for use with Krausening"""


@encrypt.command("files", options_metavar="")
@click.argument(
    "path", type=click.Path(exists=True, file_okay=False, writable=True, dir_okay=True)
)
def encrypt_directory(path: str) -> None:
    """Encrypts values of all properties files in a directory.

    Processes all *.properties files within the given path looking for keys that are prefixed with the encryption marker
    ("**").  Encrypts the value of such properties and updates the file in-place, removing the encryption marker ("**").
    """
    os.environ["KRAUSENING_BASE"] = path
    # Set to avoid warning log message
    os.environ["KRAUSENING_EXTENSIONS"] = ""
    apply_encryption()
    click.secho("\nEncryption complete.", fg="green")


if __name__ == "__main__":
    krausening()
