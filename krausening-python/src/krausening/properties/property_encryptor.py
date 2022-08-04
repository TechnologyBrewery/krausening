import base64
import hashlib
import re
import os
from Crypto.Cipher import DES


class PropertyEncryptor:
    """
    Class to mimic the standard Jasypt string encryption/decryption.

    Modified from: https://github.com/binsgit/PBEWithMD5AndDES/blob/master/python/PBEWithMD5AndDES_2.py

    See https://bitbucket.org/cpointe/krausening/src/dev/ for details on encrypting values with Jasypt.
    """

    def encrypt(self, msg: str, password: bytes) -> bytes:
        salt = os.urandom(8)
        pad_num = 8 - (len(msg) % 8)
        for i in range(pad_num):
            msg += chr(pad_num)
        (dk, iv) = self.get_derived_key(password, salt, 1000)
        crypter = DES.new(dk, DES.MODE_CBC, iv)
        enc_text = crypter.encrypt(msg)
        return base64.b64encode(salt + enc_text)

    def decrypt(self, msg: str, password: bytes) -> str:
        msg_bytes = base64.b64decode(msg)
        salt = msg_bytes[:8]
        enc_text = msg_bytes[8:]
        (dk, iv) = self.get_derived_key(password, salt, 1000)
        crypter = DES.new(dk, DES.MODE_CBC, iv)
        text = crypter.decrypt(enc_text)
        # remove the padding at the end, if any
        return re.sub(r"[\x01-\x08]", "", text.decode("utf-8"))

    def get_derived_key(self, password: bytes, salt: bytes, count: int) -> tuple:
        key = password + salt
        for i in range(count):
            m = hashlib.md5(key)
            key = m.digest()
        return (key[:8], key[8:])
