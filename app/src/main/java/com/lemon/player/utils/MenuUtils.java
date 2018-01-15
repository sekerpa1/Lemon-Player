package com.lemon.player.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.lemon.player.R;
import com.lemon.player.interfaces.FileType;
import com.lemon.player.model.Album;
import com.lemon.player.model.AlbumArtist;
import com.lemon.player.model.BaseFileObject;
import com.lemon.player.model.FileObject;
import com.lemon.player.model.FolderObject;
import com.lemon.player.model.Genre;
import com.lemon.player.model.InclExclItem;
import com.lemon.player.model.Playlist;
import com.lemon.player.model.Song;
import com.lemon.player.rx.UnsafeAction;
import com.lemon.player.rx.UnsafeCallable;
import com.lemon.player.rx.UnsafeConsumer;
import com.lemon.player.sql.databases.InclExclHelper;
import com.lemon.player.tagger.TaggerDialog;
import com.lemon.player.ui.dialog.BiographyDialog;
import com.lemon.player.ui.dialog.DeleteDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MenuUtils implements MusicUtils.Defs {

    private static final String TAG = "MenuUtils";

    // Songs

    public static void playNext(Context context, Song song) {
        MusicUtils.playNext(Collections.singletonList(song), string ->
                Toast.makeText(context, string, Toast.LENGTH_SHORT).show());
    }

    public static void newPlaylist(Context context, List<Song> songs) {
        PlaylistUtils.createPlaylistDialog(context, songs);
    }

    public static void addToPlaylist(Context context, MenuItem item, List<Song> songs) {
        Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(PlaylistUtils.ARG_PLAYLIST);
        PlaylistUtils.addToPlaylist(context, playlist, songs);
    }

    public static void showSongInfo(Context context, Song song) {
        BiographyDialog.getSongInfoDialog(context, song).show();
    }

    public static void setRingtone(Context context, Song song) {
        ShuttleUtils.setRingtone(context, song);
    }

    public static TaggerDialog editTags(Song song) {
        return TaggerDialog.newInstance(song);
    }

    public static void addToQueue(Context context, List<Song> songs) {
        MusicUtils.addToQueue(songs, message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public static void whitelist(Song song) {
        InclExclHelper.addToInclExcl(song, InclExclItem.Type.INCLUDE);
    }

    public static void whitelist(List<Song> songs) {
        InclExclHelper.addToInclExcl(songs, InclExclItem.Type.INCLUDE);
    }

    public static void blacklist(Song song) {
        InclExclHelper.addToInclExcl(song, InclExclItem.Type.EXCLUDE);
    }

    public static void blacklist(List<Song> songs) {
        InclExclHelper.addToInclExcl(songs, InclExclItem.Type.EXCLUDE);
    }

    public static void setupSongMenu(PopupMenu menu, boolean showRemoveButton) {
        menu.inflate(R.menu.menu_song);

        if (!showRemoveButton) {
            menu.getMenu().findItem(R.id.remove).setVisible(false);
        }

        // Add playlist menu
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.createPlaylistMenu(sub);
    }

    public static Toolbar.OnMenuItemClickListener getSongMenuClickListener(Context context, Single<List<Song>> songsSingle, UnsafeConsumer<DeleteDialog> deleteDialogCallback) {
        return item -> {
            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    newPlaylist(context, songsSingle);
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, songsSingle);
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, songsSingle);
                    return true;
                case R.id.blacklist:
                    blacklist(songsSingle);
                    return true;
                case R.id.delete:
                    songsSingle
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .subscribe(songs -> deleteDialogCallback.accept(DeleteDialog.newInstance(() -> songs)));
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getSongMenuClickListener(
            Context context,
            Song song,
            UnsafeConsumer<TaggerDialog> tagEditorCallback,
            UnsafeConsumer<DeleteDialog> deleteDialogCallback,
            @Nullable UnsafeAction onSongRemoved,
            @Nullable UnsafeAction onPlayNext) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.playNext:
                    if (onPlayNext != null) {
                        onPlayNext.run();
                    } else {
                        playNext(context, song);
                    }
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, Collections.singletonList(song));
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, Collections.singletonList(song));
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, Collections.singletonList(song));
                    return true;
                case R.id.editTags:
                    tagEditorCallback.accept(editTags(song));
                    return true;
                case R.id.share:
                    song.share(context);
                    return true;
                case R.id.ringtone:
                    setRingtone(context, song);
                    return true;
                case R.id.songInfo:
                    showSongInfo(context, song);
                    return true;
                case R.id.blacklist:
                    blacklist(song);
                    return true;
                case R.id.delete:
                    deleteDialogCallback.accept(DeleteDialog.newInstance(() -> Collections.singletonList(song)));
                    return true;
                case R.id.remove:
                    if (onSongRemoved != null) {
                        onSongRemoved.run();
                    }
                    return true;
            }
            return false;
        };
    }

    // Albums

    public static void setupAlbumMenu(PopupMenu menu) {
        menu.inflate(R.menu.menu_album);

        // Add playlist menu
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.createPlaylistMenu(sub);
    }

    private static Single<List<Song>> getSongsForAlbum(Album album) {
        return album.getSongsSingle()
                .map(songs -> {
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                    return songs;
                });
    }

    private static Single<List<Song>> getSongsForAlbums(List<Album> albums) {
        return Observable.fromIterable(albums)
                .flatMapSingle(MenuUtils::getSongsForAlbum)
                .reduce(Collections.emptyList(), (BiFunction<List<Song>, List<Song>, List<Song>>) (songs, songs2) -> {
                    List<Song> allSongs = new ArrayList<>();
                    allSongs.addAll(songs);
                    allSongs.addAll(songs2);
                    return allSongs;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static void play(Context context, Single<List<Song>> observable) {
        MusicUtils.playAll(observable, message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public static void newPlaylist(Context context, Single<List<Song>> single) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> PlaylistUtils.createPlaylistDialog(context, songs),
                        throwable -> LogUtils.logException(TAG, "Error adding to new playlist", throwable));
    }

    public static void addToPlaylist(Context context, MenuItem item, Single<List<Song>> single) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    Playlist playlist = (Playlist) item.getIntent().getSerializableExtra(PlaylistUtils.ARG_PLAYLIST);
                    PlaylistUtils.addToPlaylist(context, playlist, songs);
                }, throwable -> LogUtils.logException(TAG, "Error adding to playlist", throwable));
    }

    public static void addToQueue(Context context, Single<List<Song>> single) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> MusicUtils.addToQueue(songs, message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()),
                        throwable -> LogUtils.logException(TAG, "Error adding to queue", throwable));
    }

    public static TaggerDialog editTags(Album album) {
        return TaggerDialog.newInstance(album);
    }

    public static void showAlbumInfo(Context context, Album album) {
        BiographyDialog.getAlbumBiographyDialog(context, album.albumArtistName, album.name).show();
    }

    public static void showArtworkChooserDialog(Context context, Album album) {
        ArtworkDialog.build(context, album).show();
    }

    public static void whitelist(Single<List<Song>> single) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> whitelist(songs),
                        throwable -> LogUtils.logException(TAG, "whitelist failed", throwable));
    }

    public static void blacklist(Single<List<Song>> single) {
        single.observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        songs -> blacklist(songs),
                        throwable -> LogUtils.logException(TAG, "blacklist failed", throwable));
    }

    public static Toolbar.OnMenuItemClickListener getAlbumMenuClickListener(Context context, UnsafeCallable<List<Album>> callable, UnsafeConsumer<DeleteDialog> deleteDialogCallback) {
        return item -> {
            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForAlbums(callable.call()));
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, getSongsForAlbums(callable.call()));
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForAlbums(callable.call()));
                    return true;
                case R.id.delete:
                    deleteDialogCallback.accept(DeleteDialog.newInstance(callable::call));
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getAlbumMenuClickListener(Context context, Album album, UnsafeConsumer<TaggerDialog> tagEditorCallback, UnsafeConsumer<DeleteDialog> deleteDialogCallback, UnsafeAction showUpgradeDialog) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.play:
                    play(context, getSongsForAlbum(album));
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForAlbum(album));
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, getSongsForAlbum(album));
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForAlbum(album));
                    return true;
                case R.id.editTags:
                    if (!ShuttleUtils.isUpgraded()) {
                        showUpgradeDialog.run();
                    } else {
                        tagEditorCallback.accept(editTags(album));
                    }
                    return true;
                case R.id.info:
                    showAlbumInfo(context, album);
                    return true;
                case R.id.artwork:
                    showArtworkChooserDialog(context, album);
                    return true;
                case R.id.blacklist:
                    blacklist(getSongsForAlbum(album));
                    return true;
                case R.id.delete:
                    deleteDialogCallback.accept(DeleteDialog.newInstance(() -> Collections.singletonList(album)));
                    return true;
            }
            return false;
        };
    }

    // AlbumArtists

    private static Single<List<Song>> getSongsForAlbumArtist(AlbumArtist albumArtist) {
        return albumArtist.getSongsSingle()
                .map(songs -> {
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumName, b.albumName));
                    return songs;
                });
    }

    private static Single<List<Song>> getSongsForAlbumArtists(List<AlbumArtist> albumArtists) {
        return Observable.fromIterable(albumArtists)
                .flatMapSingle(MenuUtils::getSongsForAlbumArtist)
                .reduce(Collections.emptyList(), (BiFunction<List<Song>, List<Song>, List<Song>>) (songs, songs2) -> {
                    List<Song> allSongs = new ArrayList<>();
                    allSongs.addAll(songs);
                    allSongs.addAll(songs2);
                    return allSongs;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static TaggerDialog editTags(AlbumArtist albumArtist) {
        return TaggerDialog.newInstance(albumArtist);
    }

    public static void showArtistInfo(Context context, AlbumArtist albumArtist) {
        BiographyDialog.getArtistBiographyDialog(context, albumArtist.name).show();
    }

    public static void showArtworkChooserDialog(Context context, AlbumArtist albumArtist) {
        ArtworkDialog.build(context, albumArtist).show();
    }

    public static Toolbar.OnMenuItemClickListener getAlbumArtistMenuClickListener(Context context, UnsafeCallable<List<AlbumArtist>> callable, UnsafeConsumer<DeleteDialog> deleteDialogCallback) {
        return item -> {
            switch (item.getItemId()) {
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForAlbumArtists(callable.call()));
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, getSongsForAlbumArtists(callable.call()));
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForAlbumArtists(callable.call()));
                    return true;
                case R.id.delete:
                    deleteDialogCallback.accept(DeleteDialog.newInstance(callable::call));
                    return true;
            }
            return false;
        };
    }

    public static PopupMenu.OnMenuItemClickListener getAlbumArtistClickListener(Context context, AlbumArtist albumArtist, UnsafeConsumer<TaggerDialog> tagEditorCallback, UnsafeConsumer<DeleteDialog> deleteDialogCallback, UnsafeAction showUpgradeDialog) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.play:
                    play(context, getSongsForAlbumArtist(albumArtist));
                    return true;
                case R.id.albumShuffle:
                    play(context, getSongsForAlbumArtist(albumArtist).map(Operators::albumShuffleSongs));
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForAlbumArtist(albumArtist));
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, getSongsForAlbumArtist(albumArtist));
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForAlbumArtist(albumArtist));
                    return true;
                case R.id.editTags:
                    if (!ShuttleUtils.isUpgraded()) {
                        showUpgradeDialog.run();
                    } else {
                        tagEditorCallback.accept(editTags(albumArtist));
                    }
                    return true;
                case R.id.info:
                    showArtistInfo(context, albumArtist);
                    return true;
                case R.id.artwork:
                    showArtworkChooserDialog(context, albumArtist);
                    return true;
                case R.id.blacklist:
                    blacklist(getSongsForAlbumArtist(albumArtist));
                    return true;
                case R.id.delete:
                    deleteDialogCallback.accept(DeleteDialog.newInstance(() -> Collections.singletonList(albumArtist)));
                    return true;
            }
            return false;
        };
    }

    // Playlists

    public static void delete(Context context, Playlist playlist) {
        playlist.delete(context);
        Toast.makeText(context, R.string.playlist_deleted_message, Toast.LENGTH_SHORT).show();
    }

    public static void edit(Context context, Playlist playlist) {
        if (playlist.id == PlaylistUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            DialogUtils.showWeekSelectorDialog(context);
        }
    }

    public static void rename(Context context, Playlist playlist) {
        PlaylistUtils.renamePlaylistDialog(context, playlist);
    }

    public static void export(Context context, Playlist playlist) {
        PlaylistUtils.createM3uPlaylist(context, playlist);
    }

    public static void clear(Playlist playlist) {
        playlist.clear();
    }

    public static void setupPlaylistMenu(PopupMenu menu, Playlist playlist) {
        menu.inflate(R.menu.menu_playlist);

        if (!playlist.canDelete) {
            menu.getMenu().findItem(R.id.deletePlaylist).setVisible(false);
        }

        if (!playlist.canClear) {
            menu.getMenu().findItem(R.id.clearPlaylist).setVisible(false);
        }

        if (playlist.id != PlaylistUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            menu.getMenu().findItem(R.id.editPlaylist).setVisible(false);
        }

        if (!playlist.canRename) {
            menu.getMenu().findItem(R.id.renamePlaylist).setVisible(false);
        }

        if (playlist.id == PlaylistUtils.PlaylistIds.MOST_PLAYED_PLAYLIST) {
            menu.getMenu().findItem(R.id.exportPlaylist).setVisible(false);
        }
    }

    public static void setupPlaylistMenu(Toolbar toolbar, Playlist playlist) {
        toolbar.inflateMenu(R.menu.menu_playlist);

        if (!playlist.canDelete) {
            toolbar.getMenu().findItem(R.id.deletePlaylist).setVisible(false);
        }

        if (!playlist.canClear) {
            toolbar.getMenu().findItem(R.id.clearPlaylist).setVisible(false);
        }

        if (playlist.id != PlaylistUtils.PlaylistIds.RECENTLY_ADDED_PLAYLIST) {
            toolbar.getMenu().findItem(R.id.editPlaylist).setVisible(false);
        }

        if (!playlist.canRename) {
            toolbar.getMenu().findItem(R.id.renamePlaylist).setVisible(false);
        }

        if (playlist.id == PlaylistUtils.PlaylistIds.MOST_PLAYED_PLAYLIST) {
            toolbar.getMenu().findItem(R.id.exportPlaylist).setVisible(false);
        }
    }

    public static PopupMenu.OnMenuItemClickListener getPlaylistPopupMenuClickListener(final Context context, final Playlist playlist, @Nullable UnsafeAction playlistDeleted) {
        return item -> handleMenuItemClicks(context, item, playlist, playlistDeleted);
    }

    public static boolean handleMenuItemClicks(Context context, MenuItem menuItem, Playlist playlist, @Nullable UnsafeAction playlistDeleted) {
        switch (menuItem.getItemId()) {
            case R.id.playPlaylist:
                play(context, playlist.getSongsObservable().first(Collections.emptyList()));
                return true;
            case R.id.deletePlaylist:
                delete(context, playlist);
                if (playlistDeleted != null) {
                    playlistDeleted.run();
                }
                return true;
            case R.id.editPlaylist:
                edit(context, playlist);
                return true;
            case R.id.renamePlaylist:
                rename(context, playlist);
                return true;
            case R.id.exportPlaylist:
                export(context, playlist);
                return true;
            case R.id.clearPlaylist:
                clear(playlist);
                return true;
        }
        return false;
    }

    // Genres

    private static Single<List<Song>> getSongsForGenre(Genre genre) {
        return genre.getSongsObservable()
                .map(songs -> {
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(b.year, a.year));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.track, b.track));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compareInt(a.discNumber, b.discNumber));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumName, b.albumName));
                    Collections.sort(songs, (a, b) -> ComparisonUtils.compare(a.albumArtistName, b.albumArtistName));
                    return songs;
                });
    }

    public static PopupMenu.OnMenuItemClickListener getGenreClickListener(final Context context, final Genre genre) {
        return item -> {
            switch (item.getItemId()) {
                case R.id.play:
                    play(context, getSongsForGenre(genre));
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForGenre(genre));
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, item, getSongsForGenre(genre));
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForGenre(genre));
                    return true;
            }
            return false;
        };
    }

    // Folders

    static Single<Song> getSongForFile(FileObject fileObject) {
        return FileHelper.getSong(new File(fileObject.path))
                .observeOn(AndroidSchedulers.mainThread());
    }

    static Single<List<Song>> getSongsForFolderObject(FolderObject folderObject) {
        return FileHelper.getSongList(new File(folderObject.path), true, false);
    }

    public static void setupFolderMenu(PopupMenu menu, BaseFileObject fileObject) {

        menu.inflate(R.menu.menu_file);

        // Add playlist menu
        SubMenu sub = menu.getMenu().findItem(R.id.addToPlaylist).getSubMenu();
        PlaylistUtils.createPlaylistMenu(sub);

        if (!fileObject.canReadWrite()) {
            menu.getMenu().findItem(R.id.rename).setVisible(false);
        }

        switch (fileObject.fileType) {
            case FileType.FILE:
                menu.getMenu().findItem(R.id.play).setVisible(false);
                menu.getMenu().findItem(R.id.setInitialDir).setVisible(false);
                break;
            case FileType.FOLDER:
                menu.getMenu().findItem(R.id.playNext).setVisible(false);
                menu.getMenu().findItem(R.id.songInfo).setVisible(false);
                menu.getMenu().findItem(R.id.ringtone).setVisible(false);
                menu.getMenu().findItem(R.id.share).setVisible(false);
                menu.getMenu().findItem(R.id.editTags).setVisible(false);
                break;
            case FileType.PARENT:
                break;
        }
    }

    public static void scanFile(Context context, FileObject fileObject) {
        CustomMediaScanner.scanFile(fileObject.path, message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public static void scanFolder(Context context, FolderObject folderObject) {
        CustomMediaScanner.scanFile(context, folderObject);
    }

    public static void renameFile(Context context, BaseFileObject fileObject, UnsafeAction filenameChanged) {

        @SuppressLint("InflateParams")
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null);

        final EditText editText = customView.findViewById(R.id.editText);
        editText.setText(fileObject.name);

        MaterialDialog.Builder builder = DialogUtils.getBuilder(context);
        if (fileObject.fileType == FileType.FILE) {
            builder.title(R.string.rename_file);
        } else {
            builder.title(R.string.rename_folder);
        }

        builder.customView(customView, false);
        builder.positiveText(R.string.save)
                .onPositive((materialDialog, dialogAction) -> {
                    if (editText.getText() != null) {
                        if (FileHelper.renameFile(context, fileObject, editText.getText().toString())) {
                            filenameChanged.run();
                        } else {
                            Toast.makeText(context,
                                    fileObject.fileType == FileType.FOLDER ? R.string.rename_folder_failed : R.string.rename_file_failed,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
        builder.negativeText(R.string.cancel)
                .show();
    }

    public static void deleteFile(Context context, BaseFileObject fileObject, UnsafeAction fileDeleted) {
        MaterialDialog.Builder builder = DialogUtils.getBuilder(context)
                .title(R.string.delete_item)
                .iconRes(R.drawable.ic_warning_24dp);
        if (fileObject.fileType == FileType.FILE) {
            builder.content(String.format(context.getResources().getString(
                    R.string.delete_file_confirmation_dialog), fileObject.name));
        } else {
            builder.content(String.format(context.getResources().getString(
                    R.string.delete_folder_confirmation_dialog), fileObject.path));
        }
        builder.positiveText(R.string.button_ok)
                .onPositive((materialDialog, dialogAction) -> {
                    if (FileHelper.deleteFile(new File(fileObject.path))) {
                        fileDeleted.run();
                        CustomMediaScanner.scanFiles(Collections.singletonList(fileObject.path), null);
                    } else {
                        Toast.makeText(context,
                                fileObject.fileType == FileType.FOLDER ? R.string.delete_folder_failed : R.string.delete_file_failed,
                                Toast.LENGTH_LONG).show();
                    }
                });
        builder.negativeText(R.string.cancel)
                .show();
    }

    public static void setInitialDir(Context context, FolderObject folderObject) {
        SettingsManager.getInstance().setFolderBrowserInitialDir(folderObject.path);
        Toast.makeText(context, folderObject.path + context.getResources().getString(R.string.initial_dir_set_message), Toast.LENGTH_SHORT).show();
    }

    @Nullable
    public static PopupMenu.OnMenuItemClickListener getFolderMenuClickListener(Context context, BaseFileObject fileObject, UnsafeConsumer<TaggerDialog> tagEditorCallback, UnsafeAction filenameChanged, UnsafeAction fileDeleted) {
        switch (fileObject.fileType) {
            case FileType.FILE:
                return getFileMenuClickListener(context, (FileObject) fileObject, tagEditorCallback, filenameChanged, fileDeleted);
            case FileType.FOLDER:
                return getFolderMenuClickListener(context, (FolderObject) fileObject, filenameChanged, fileDeleted);
        }
        return null;
    }

    private static PopupMenu.OnMenuItemClickListener getFileMenuClickListener(Context context, FileObject fileObject, UnsafeConsumer<TaggerDialog> tagEditorCallback, UnsafeAction filenameChanged, UnsafeAction fileDeleted) {
        return menuItem -> {

            Consumer<Throwable> errorHandler = e -> LogUtils.logException(TAG, "getFileMenuClickListener threw error", e);

            switch (menuItem.getItemId()) {
                case R.id.playNext:
                    getSongForFile(fileObject).subscribe(song -> playNext(context, song), errorHandler);
                    return true;
                case NEW_PLAYLIST:
                    getSongForFile(fileObject).subscribe(song -> newPlaylist(context, Collections.singletonList(song)), errorHandler);
                    return true;
                case PLAYLIST_SELECTED:
                    getSongForFile(fileObject).subscribe(song -> addToPlaylist(context, menuItem, Collections.singletonList(song)), errorHandler);
                    return true;
                case R.id.addToQueue:
                    getSongForFile(fileObject).subscribe(song -> addToQueue(context, Collections.singletonList(song)), errorHandler);
                    return true;
                case R.id.scan:
                    scanFile(context, fileObject);
                    return true;
                case R.id.editTags:
                    getSongForFile(fileObject).subscribe(song -> tagEditorCallback.accept(editTags(song)), errorHandler);
                    return true;
                case R.id.share:
                    getSongForFile(fileObject).subscribe(song -> song.share(context), errorHandler);
                    return true;
                case R.id.ringtone:
                    getSongForFile(fileObject).subscribe(song -> setRingtone(context, song), errorHandler);
                    return true;
                case R.id.songInfo:
                    getSongForFile(fileObject).subscribe(song -> showSongInfo(context, song), errorHandler);
                    return true;
                case R.id.blacklist:
                    getSongForFile(fileObject).subscribe(song -> blacklist(song), errorHandler);
                case R.id.whitelist:
                    getSongForFile(fileObject).subscribe(song -> whitelist(song), errorHandler);
                    return true;
                case R.id.rename:
                    renameFile(context, fileObject, filenameChanged);
                    return true;
                case R.id.delete:
                    deleteFile(context, fileObject, fileDeleted);
                    return true;
            }
            return false;
        };
    }

    private static PopupMenu.OnMenuItemClickListener getFolderMenuClickListener(Context context, FolderObject folderObject, UnsafeAction filenameChanged, UnsafeAction fileDeleted) {
        return menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.play:
                    play(context, getSongsForFolderObject(folderObject));
                    return true;
                case NEW_PLAYLIST:
                    newPlaylist(context, getSongsForFolderObject(folderObject));
                    return true;
                case PLAYLIST_SELECTED:
                    addToPlaylist(context, menuItem, getSongsForFolderObject(folderObject));
                    return true;
                case R.id.addToQueue:
                    addToQueue(context, getSongsForFolderObject(folderObject));
                    return true;
                case R.id.setInitialDir:
                    setInitialDir(context, folderObject);
                    return true;
                case R.id.scan:
                    scanFolder(context, folderObject);
                    return true;
                case R.id.whitelist:
                    whitelist(getSongsForFolderObject(folderObject));
                case R.id.blacklist:
                    blacklist(getSongsForFolderObject(folderObject));
                    return true;
                case R.id.rename:
                    renameFile(context, folderObject, filenameChanged);
                    return true;
                case R.id.delete:
                    deleteFile(context, folderObject, fileDeleted);
                    return true;
            }
            return false;
        };
    }
}