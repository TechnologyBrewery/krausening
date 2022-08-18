import os
from javaproperties import Properties as JavaProperties
from krausening.logging import LogManager
from krausening.properties import PropertyEncryptor
from typing import Optional, TypeVar

T = TypeVar("T")


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

    def get_properties(self, file_name: str):
        base = os.environ.get("KRAUSENING_BASE", None)
        extension = os.environ.get("KRAUSENING_EXTENSIONS", None)
        password = os.environ.get("KRAUSENING_PASSWORD", None)
        if password is not None:
            properties = EncryptableProperties(password)
        else:
            properties = Properties()

        if base is not None:
            try:
                if not base.endswith("/"):
                    base = base + "/"
                properties.load(open("{0}{1}".format(base, file_name)))
            except FileNotFoundError:
                self._logger.warn(
                    "No base file found for {0}{1}".format(base, file_name)
                )

        if extension is not None:
            try:
                if not extension.endswith("/"):
                    extension = extension + "/"
                properties.load(open("{0}{1}".format(extension, file_name)))
            except FileNotFoundError:
                self._logger.warn(
                    "No extension file found for {0}{1}".format(base, file_name)
                )

        return properties


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
            return self.data[key]
        except KeyError:
            if self.defaults is not None:
                return self.defaults.getProperty(key, defaultValue)
            else:
                return defaultValue


class EncryptableProperties(Properties):
    """
    This class represents a properties file that can decrypt property values that have been encrypted
    with Jasypt, and is based on Jaspyt's EncryptableProperties.

    See https://bitbucket.org/cpointe/krausening/src/dev/ for details on encrypting values with Jasypt.
    """

    def __init__(self, password: str) -> None:
        super().__init__()
        self.__propertyPrefix = "ENC("
        self.__propertySuffix = ")"
        self.__encryptor = PropertyEncryptor()
        self.__password = password

    def getProperty(self, key: str, defaultValue: Optional[T] = None):
        value = super().getProperty(key)
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

        return value
