import base64
import re
import os

from cryptography.hazmat.primitives.hashes import Hash, MD5
from cryptography.hazmat.primitives.ciphers import Cipher
from cryptography.hazmat.primitives.ciphers.algorithms import TripleDES
from cryptography.hazmat.primitives.ciphers.modes import CBC


class PropertyEncryptor:
    """
    Provides property value encryption/decryption support via PBEWithMD5AndDES, which is the
    default encryption algorithm used by Jasypt's CLI and StandardPBEByteEncryptor. This aligns with
    the same approach used for property encryption within the Krausening Java package.

    As per https://fermenter.atlassian.net/browse/KRAUS-11, later iterations should seek to introduce
    a more secure encryption algorithm approach.

    See https://bitbucket.org/cpointe/krausening/src/dev/ for details on encrypting values with Jasypt.
    """

    def encrypt(self, value_to_encrypt: str, password: bytes) -> bytes:
        salt = os.urandom(8)
        (key, init_vector) = self._pbkdf1_md5(password, salt, 1000)
        cipher = Cipher(TripleDES(key), CBC(init_vector))
        encryptor = cipher.encryptor()
        return encryptor.update(value_to_encrypt) + encryptor.finalize()

    def decrypt(self, encrypted_msg: str, password: bytes) -> str:
        msg_bytes = base64.b64decode(encrypted_msg)
        salt = msg_bytes[:8]
        enc_text = msg_bytes[8:]
        (key, init_vector) = self._pbkdf1_md5(password, salt, 1000)

        cipher = Cipher(TripleDES(key), CBC(init_vector))
        decryptor = cipher.decryptor()
        text = decryptor.update(enc_text) + decryptor.finalize()
        # remove the padding at the end, if any
        return re.sub(r"[\x01-\x08]", "", text.decode("utf-8"))

    def _pbkdf1_md5(self, password, salt, iterations):
        """
        Provides a Password Based Key Derivation Function (PBKDF1) as defined in RFC 2829
        (https://www.rfc-editor.org/rfc/rfc2898#section-5.1) that applies a MD5 hash function
        to derive a key from the given password.
        """
        digest = Hash(MD5())
        digest.update(password)
        digest.update(salt)

        key = None
        for i in range(iterations):
            key = digest.finalize()
            digest = Hash(MD5())
            digest.update(key)

        digest.finalize()

        return key[:8], key[8:16]
