#!/usr/bin/python3
"""Pull public, checksum-verified images; no GitHub token or SSH key required."""
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import urllib.error
import urllib.request

BASE = Path('/opt/nawyki')
REPOSITORY = 'sirdobro/nawyki'

def request(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers={
        'User-Agent': 'nawyki-release-puller', 'Accept': 'application/vnd.github+json'
    }), timeout=60)

def main():
    try:
        with request(f'https://api.github.com/repos/{REPOSITORY}/releases/latest') as response:
            release = json.load(response)
    except urllib.error.HTTPError as error:
        if error.code == 404:
            print('No published deployment yet.')
            return
        raise
    tag = release.get('tag_name', '')
    if not re.fullmatch(r'deploy-[0-9a-f]{40}', tag) or release.get('draft') or release.get('prerelease'):
        raise ValueError('Latest release is not a production deployment.')
    sha = tag.removeprefix('deploy-')
    current = BASE / 'release.env'
    if current.exists() and f'APP_RELEASE={sha}\n' in current.read_text():
        print('Already at release', sha)
        return
    assets = {asset['name']: asset for asset in release['assets']}
    prefix = f'https://github.com/{REPOSITORY}/releases/download/{tag}/'
    urls = {}
    for name in ['nawyki-image.tar.gz', 'nawyki-image.sha256']:
        asset = assets[name]
        if asset['browser_download_url'] != prefix + name:
            raise ValueError('Unexpected release asset URL.')
        if asset['size'] > (512 * 1024 * 1024 if name.endswith('.gz') else 1024):
            raise ValueError('Release asset too large.')
        urls[name] = asset['browser_download_url']
    with request(urls['nawyki-image.sha256']) as response:
        checksum = response.read(1025).decode().strip()
    if not re.fullmatch(r'[0-9a-f]{64}  nawyki-image\.tar\.gz', checksum):
        raise ValueError('Invalid checksum file.')
    expected = checksum.split()[0]
    with tempfile.TemporaryDirectory(prefix='release-', dir=BASE) as temporary:
        archive = Path(temporary) / 'image.tar.gz'
        digest = hashlib.sha256()
        total = 0
        with request(urls['nawyki-image.tar.gz']) as response, archive.open('wb') as output:
            while chunk := response.read(1024 * 1024):
                total += len(chunk)
                if total > 512 * 1024 * 1024:
                    raise ValueError('Download exceeded maximum size.')
                output.write(chunk)
                digest.update(chunk)
        if digest.hexdigest() != expected:
            raise ValueError('Image checksum mismatch; refusing deployment.')
        with archive.open('rb') as image:
            subprocess.run(['/usr/local/sbin/nawyki-deploy', sha], stdin=image, check=True, timeout=420)
    print('Deployment verified on the VPS:', sha)

if __name__ == '__main__':
    main()
