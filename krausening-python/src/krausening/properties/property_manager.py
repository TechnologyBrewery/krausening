import os
from javaproperties import Properties as JavaProperties
from krausening.logging import LogManager
from krausening.properties import PropertyEncryptor
from typing import Optional, TypeVar
from platform import uname
from watchdog.events import FileSystemEventHandler
from watchdog.observers import Observer


def in_wsl() -> bool:
    return "microsoft-standard" in uname().release


# Use polling observer if in WSL due to watchdog observer currently not correctly detecting changes
if in_wsl():
    from watchdog.observers.polling import PollingObserver as Observer

T = TypeVar("T")


class FileUpdateEventHandler(FileSystemEventHandler):
    def __init__(self, base_path):
        self._logger = LogManager.get_instance().get_logger("FileWatcher")
        self._base_path = os.path.abspath(base_path)
        self._logger.info(f"File Watcher started on {self._base_path}")

    def on_modified(self, event):
        file_path = event.src_path.replace(self._base_path, "")

        ## testing for WSL since watchdog acts differently with WSL else continue as normal
        ## if in WSL we need only the file name and not the whole path
        if in_wsl():
            last_index = file_path.rfind("/")
            if last_index != -1:
                self._logger.info(
                    f"Updating file_path to: {file_path[last_index + 1 :]}"
                )
                file_path = file_path[last_index + 1 :]

        else:
            if file_path.startswith("/"):
                self._logger.info(f"Updating file_path to: {file_path[1:]}")
                file_path = file_path[1:]

        if PropertyManager.get_instance().is_loaded(file_path):
            self._logger.warning(
                f"Detected a file update in {event.src_path}!  Triggering update..."
            )
            PropertyManager.get_instance().get_properties(file_path, force_reload=True)


class PropertyManager:
    """
    Class for handling External Property Configurations.
    """

    __instance = None

    @staticmethod
    def get_instance():
        if PropertyManager.__instance is None:
            PropertyManager()
        return PropertyManager.__instance

    def __init__(self):
        if PropertyManager.__instance is not None:
            raise Exception("Class is a singleton")
        else:
            PropertyManager.__instance = self
        self._logger = LogManager.get_instance().get_logger("PropertyManager")
        self._property_cache = {}

        if os.environ.get("KRAUSENING_BASE", None) is not None:
            self._base_observer = Observer()
            self._base_observer.schedule(
                FileUpdateEventHandler(os.environ.get("KRAUSENING_BASE")),
                os.environ.get("KRAUSENING_BASE"),
                recursive=True,
            )
            self._base_observer.start()

        if (
            os.environ.get("KRAUSENING_EXTENSIONS", None) is not None
            and os.environ.get("KRAUSENING_EXTENSIONS") != ""
        ):
            self._extension_observer = Observer()
            self._extension_observer.schedule(
                FileUpdateEventHandler(os.environ.get("KRAUSENING_EXTENSIONS")),
                os.environ.get("KRAUSENING_EXTENSIONS"),
                recursive=True,
            )
            self._extension_observer.start()

        if (
            os.environ.get("KRAUSENING_OVERRIDE_EXTENSIONS", None) is not None
            and os.environ.get("KRAUSENING_OVERRIDE_EXTENSIONS") != ""
        ):
            self._extension_observer = Observer()
            self._extension_observer.schedule(
                FileUpdateEventHandler(
                    os.environ.get("KRAUSENING_OVERRIDE_EXTENSIONS")
                ),
                os.environ.get("KRAUSENING_OVERRIDE_EXTENSIONS"),
                recursive=True,
            )
            self._extension_observer.start()

    def get_properties(self, file_name: str, force_reload=False):
        if file_name in self._property_cache and not force_reload:
            return self._property_cache[file_name]

        base = os.environ.get("KRAUSENING_BASE", None)
        extension = os.environ.get("KRAUSENING_EXTENSIONS", None)
        override = os.environ.get("KRAUSENING_OVERRIDE_EXTENSIONS", None)
        password = os.environ.get("KRAUSENING_PASSWORD", None)
        if password is not None:
            properties = EncryptableProperties(password)
        else:
            properties = Properties()

        if file_name not in self._property_cache:
            self._property_cache[file_name] = properties

        if base is not None:
            try:
                if not base.endswith("/"):
                    base = base + "/"
                properties.load(open("{0}{1}".format(base, file_name)))
                self._validate_property_names(properties)
            except FileNotFoundError:
                self._logger.warning(
                    "No base file found for {0}{1}".format(base, file_name)
                )

        if extension is not None:
            try:
                if not extension.endswith("/"):
                    extension = extension + "/"
                properties.load(open("{0}{1}".format(extension, file_name)))
                self._validate_property_names(properties)
            except FileNotFoundError:
                self._logger.warning(
                    "No extension file found for {0}{1}".format(base, file_name)
                )

        if override is not None:
            try:
                if not override.endswith("/"):
                    override = override + "/"
                properties.load(open("{0}{1}".format(override, file_name)))
                self._validate_property_names(properties)
            except FileNotFoundError:
                self._logger.warning(
                    "No extension file found for {0}{1}".format(base, file_name)
                )

        self._property_cache[file_name].update(properties)

        return self._property_cache[file_name]

    def is_loaded(self, file_name):
        return file_name in self._property_cache

    def _validate_property_names(self, properties):
        properties.propertyNames()
        encryption_marked_keys = [
            name for name in properties.propertyNames() if name.startswith("**")
        ]
        if len(encryption_marked_keys) > 0:
            raise ValueError(
                f'There are unencrypted keys in the .properties file. Follow the "Properties Encryption" instruction in the krausening-python/README to encrypt the keys: {encryption_marked_keys}'
            )


class Properties(JavaProperties):
    """
    This class represents a properties file without encryption
    """

    def __init__(self) -> None:
        super().__init__()

    def __getitem__(self, key: str) -> str:
        return self.getProperty(key)

    def getProperty(self, key: str, defaultValue: Optional[T] = None):
        try:
            return os.path.expandvars(self.data[key])
        except KeyError:
            if self.defaults is not None:
                return os.path.expandvars(self.defaults.getProperty(key, defaultValue))
            else:
                return os.path.expandvars(defaultValue)


class EncryptableProperties(Properties):
    """
    Provides property value encryption/decryption support via PBEWITHHMACSHA512ANDAES_256. This aligns with
    the same approach used for property encryption within the Krausening Java package.

    Reference: https://resultfor.dev/359470-implement-pbewithhmacsha512andaes-256-of-java-jasypt-in-python.

    See https:https://github.com/TechnologyBrewery/krausening/tree/dev/krausening for details on encrypting values with Jasypt.
    """

    def __init__(self, password: str) -> None:
        super().__init__()
        self.__propertyPrefix = "ENC("
        self.__propertySuffix = ")"
        self.__encryptor = PropertyEncryptor()
        self.__password = password

    def getProperty(self, key: str, defaultValue: Optional[T] = None):
        value = super().getProperty(key, defaultValue)
        if value is not None:
            if value.startswith(self.__propertyPrefix) and value.endswith(
                self.__propertySuffix
            ):
                encrypted_value = value
                # remove prefix from value
                encrypted_value = encrypted_value[len(self.__propertyPrefix) :]
                # remove suffix from value
                encrypted_value = encrypted_value[: -len(self.__propertySuffix)]

                # decrypt the value
                password_bytes = self.__password.encode()
                value = self.__encryptor.decrypt(encrypted_value, password_bytes)

        return os.path.expandvars(value)

    def _encrypt(self, key: str) -> bytes:
        value = self.get(key)
        password_bytes = self.__password.encode()
        encrypted_value = self.__encryptor.encrypt(value, password_bytes)
        return encrypted_value

    def _decrypt(self, value: str):
        password_bytes = self.__password.encode()
        decrypted_value = self.__encryptor.decrypt(value, password_bytes)
        return decrypted_value


class PropertiesEncryptor:
    """
    Provide encryption support for any property key marked with encryption mark `**`
    """

    def __init__(self):
        self._logger = LogManager.get_instance().get_logger("PropertiesEncryptor")
        self._encryptor = PropertyEncryptor()
        self._encryption_mark = "**"

    def apply_encryption(self):
        """
        Apply encryption to all marked key (e.g.: **key1=value) values defined in the .properties files within the
        `KRAUSENING_BASE`, `KRAUSENING_EXTENSIONS` and `KRAUSENING_OVERRIDE_EXTENSIONS` directories. The key's encryption
        mark('**`) will be removed after the encryption is applied.
        """
        password = os.environ.get("KRAUSENING_PASSWORD", None)
        if password is None:
            raise ValueError(
                "Missing environment variable `KRAUSENING_PASSWORD` for value encryption."
            )

        base = os.environ.get("KRAUSENING_BASE", None)
        extension = os.environ.get("KRAUSENING_EXTENSIONS", None)
        override = os.environ.get("KRAUSENING_OVERRIDE_EXTENSIONS", None)

        if base is not None:
            if not base.endswith("/"):
                base = base + "/"
            self._apply_encryption_from_location(base, "KRAUSENING_BASE", password)
        else:
            self._logger.warning(
                "Without a KRAUSENING_BASE set, Krausening cannot load any properties!"
            )

        if extension is not None:
            if not extension.endswith("/"):
                extension = extension + "/"
            self._apply_encryption_from_location(
                extension, "KRAUSENING_EXTENSIONS", password
            )
        else:
            self._logger.warning("No KRAUSENING_EXTENSIONS set..")

        if override is not None:
            if not override.endswith("/"):
                override = override + "/"
            self._apply_encryption_from_location(
                override, "KRAUSENING_OVERRIDE_EXTENSIONS", password
            )

    def _apply_encryption_from_location(
        self, location: str, location_type: str, password: str
    ):
        if not os.path.exists(location):
            self._logger.warning(
                f"{location_type} refers to a location that does not exist: {os.path.abspath(location)}"
            )
            return

        # get all config files
        files = []
        for file_name in os.listdir(location):
            full_path = os.path.join(location, file_name)
            if os.path.isfile(full_path) and file_name.endswith(".properties"):
                files.append(full_path)

        # apply encryption to all property files
        for file in files:
            lines_in = []
            try:
                with open(file, "r") as f:
                    lines_in = f.readlines()
            except Exception as e:
                self._logger.error(
                    "Fail to read the {0} file for encryption: {1}".format(file, e)
                )

            lines_out = []
            encrypted_keys = []
            for line_in in lines_in:
                # Would be nice to preserve their spacing
                line = line_in.lstrip()
                if self._should_encrypt(line):
                    key, value = self._parse_property(line)
                    # Java Properties ignores whitespace before values so remove, but preserve trailing whitespace
                    value = f"ENC({self._encryptor.encrypt(value.lstrip(), password.encode())})"
                    line = f"{key}={value}\n"
                    encrypted_keys.append(key)

                lines_out.append(line)
            # write to the properties file
            if encrypted_keys:
                try:
                    with open(file, "w") as f:
                        for line in lines_out:
                            f.write(line)
                    self._logger.info(
                        f"Applied encryption to {location_type} {file} for keys: {encrypted_keys}"
                    )

                except Exception as e:
                    self._logger.error(
                        f"Fail to apply encryption to {location_type} file at: {file} with {e}"
                    )

    def _should_encrypt(self, line):
        return line and line.startswith(self._encryption_mark)

    def _parse_property(self, line):
        marklen = len(self._encryption_mark)
        # breaks if name contains escaped =, or uses : as separator
        split = line.split("=", 1)
        key = split[0][marklen:]
        value = split[1]
        return key, value


def apply_encryption():
    """
    Apply secret encryption to all .properties file defined in the KRAUSENING_BASE, KRAUSENING_EXTENSIONS, KRAUSENING_OVERRIDE_EXTENSIONS
    """
    property_secret_encryptor = PropertiesEncryptor()
    property_secret_encryptor.apply_encryption()
