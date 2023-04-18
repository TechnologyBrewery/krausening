import os
from base64 import b64decode, b64encode
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives.padding import PKCS7
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes


class PropertyEncryptor:
    """
    Provides property value encryption/decryption support via PBEWITHHMACSHA512ANDAES_256. This aligns with
    the same approach used for property encryption within the Krausening Java package.

    Reference: https://resultfor.dev/359470-implement-pbewithhmacsha512andaes-256-of-java-jasypt-in-python.

    See https://github.com/TechnologyBrewery/krausening/tree/dev/krausening for details on encrypting values with Jasypt.
    """

    def encrypt(self, value_to_encrypt: str, password: bytes) -> bytes:
        return self.encrypt_pbe_with_hmac_sha512_aes_256(value_to_encrypt, password)

    def decrypt(self, encrypted_msg: str, password: bytes) -> str:
        return self.decrypt_pbe_with_hmac_sha512_aes_256(encrypted_msg, password)

    def decrypt_pbe_with_hmac_sha512_aes_256(
        self, encrypted_msg: str, masterKey: bytes
    ) -> str:
        # re-generate key from
        encrypted_obj = b64decode(encrypted_msg)
        salt = encrypted_obj[0:16]
        iv = encrypted_obj[16:32]
        cypher_text = encrypted_obj[32:]
        kdf = PBKDF2HMAC(hashes.SHA512(), 32, salt, 1000, backend=default_backend())
        key = kdf.derive(masterKey)

        # decrypt
        cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
        decryptor = cipher.decryptor()
        padded_text = decryptor.update(cypher_text) + decryptor.finalize()

        # remove padding
        unpadder = PKCS7(128).unpadder()
        clear_text = unpadder.update(padded_text) + unpadder.finalize()
        return clear_text.decode()

    def encrypt_pbe_with_hmac_sha512_aes_256(
        self, value_to_encrypt: str, masterKey: bytes
    ) -> str:
        # generate key
        salt = os.urandom(16)
        iv = os.urandom(16)
        kdf = PBKDF2HMAC(hashes.SHA512(), 32, salt, 1000, backend=default_backend())
        key = kdf.derive(masterKey)

        # pad data
        padder = PKCS7(128).padder()
        data = padder.update(value_to_encrypt.encode()) + padder.finalize()

        # encrypt
        cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
        encryptor = cipher.encryptor()
        cypher_text = encryptor.update(data) + encryptor.finalize()

        return b64encode(salt + iv + cypher_text).decode()
